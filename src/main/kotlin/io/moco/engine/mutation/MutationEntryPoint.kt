/*
 * Copyright (c) 2021. Tran Phan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.moco.engine.mutation

import io.moco.engine.ClassName
import io.moco.engine.Configuration
import io.moco.engine.WorkerProcess
import io.moco.engine.operator.Operator
import io.moco.engine.preprocessing.PreprocessStorage
import io.moco.engine.test.SerializableTestInfo
import io.moco.persistence.*
import io.moco.utils.ByteArrayLoader
import io.moco.utils.GitProcessor
import io.moco.utils.MoCoLogger
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * Mutation entry point
 *
 * @property byteArrLoader
 * @property mutationStorage
 * @property gitProcessor
 * @property createdAgentLocation
 * @property clsByGit
 * @property lastRunIDFromMeta
 * @property fOpNames
 * @property mocoBuildPath
 * @property mutationResultsFolder
 * @property buildRoot
 * @property gitMode
 * @constructor Create empty Mutation entry point
 */
class MutationEntryPoint(
    private val byteArrLoader: ByteArrayLoader,
    private val mutationStorage: MutationStorage,
    private val gitProcessor: GitProcessor?,
    private val createdAgentLocation: String?,
    private val clsByGit: List<String>?,
    private val lastRunIDFromMeta: String?,
    private val fOpNames: List<String> = Configuration.currentConfig!!.fOpNames,
    private val mocoBuildPath: String = Configuration.currentConfig!!.mocoBuildPath,
    private val mutationResultsFolder: String = Configuration.currentConfig!!.mutationResultsFolder,
    private val buildRoot: String = Configuration.currentConfig!!.buildRoot,
    private val gitMode: Boolean = Configuration.currentConfig!!.gitMode,
) {
    val logger = MoCoLogger()

    fun mutationTest(newOperatorsSelected: Boolean) {
        try {
            val preprocessedStorage = PreprocessStorage.getPreprocessStorage(mocoBuildPath)
            if (preprocessedStorage == null) {
                logger.info("No preprocess results, skip mutation testing")
                return
            }
            if (preprocessedStorage.classRecord.isNullOrEmpty()) {
                logger.info("No preprocess information available, skip mutation testing")
                return
            }
            // persist preprocess result to database
            TestsCutMapping().saveMappingInfo(preprocessedStorage.classRecord)
            // Mutations collecting
            logger.debug("Start mutation collecting")

            val mGen = MutationGenerator(byteArrLoader, fOpNames.mapNotNull { Operator.nameToOperator(it) })
            val foundMutations: MutableMap<String, List<Mutation>> = mutableMapOf()
            for (item in preprocessedStorage.classRecord) {
                foundMutations[item.classUnderTestName] =
                    mGen.findPossibleMutationsOfClass(item.classUnderTestName, item.coveredLines?.keys,
                                                     Configuration.currentConfig!!.filterMutants).distinct()
            }
            val filteredMutations = filterMutations(foundMutations, newOperatorsSelected)
            logger.debug("Found ${filteredMutations.size} class(es) can be mutated")
            logger.debug("Complete mutation collecting step")

            // Mutants generation and tests execution
            logger.debug("Start mutation testing")
            handleMutations(filteredMutations, preprocessedStorage)

            persistMutationResults(preprocessedStorage)

            logger.debug("Complete mutation testing")
        } catch (ex: Exception) {
            logger.error("Error while executing mutation test phase - ${ex.message}")
            logger.error(ex.printStackTrace().toString())
        }
    }

    /**
     * Filter mutations
     *
     * @param mutations
     * @param newOperatorsSelected
     * @return
     *
     * This method filter out mutations from collected mutations list
     * for now, we only remove mutations that were recorded in mutants black list
     * Before performing the filtering, blacklisted mutants of the changed class (by Git commit) are deleted from DB
     */
    private fun filterMutations(
        mutations: MutableMap<String, List<Mutation>>,
        newOperatorsSelected: Boolean
    ): Map<String, List<Mutation>> {
        if (gitMode && !newOperatorsSelected) {
            if (!clsByGit.isNullOrEmpty()) {
                // Mutants black list UPDATE - REMOVAL
                // remove black listed mutants of the changed classes by Git because their contents have been changed
                MutantsBlackList().removeData("className IN (\'${clsByGit.joinToString("\',\'")}\')")
            }
            val mutantsBlackList = MutantsBlackList().getData("")
            for (item in mutantsBlackList) {
                val bLEntry = item.entry
                if (mutations.keys.any { it == bLEntry["className"] }) {
                    // Mutants black list USAGE
                    val mList = mutations[bLEntry["className"]]?.toMutableList()
                    for (m in mList!!) {
                        if (m.lineOfCode.toString() == bLEntry["loc"]
                            && m.mutationID.instructionIndices!!.joinToString(",") == bLEntry["instructionIndices"]
                            && m.mutationID.mutatorID == bLEntry["mutatorID"]
                        ) {
                            mList.remove(m)
                        }
                    }
                }
            }
        }
        return mutations.filter { it.value.isNotEmpty() }
    }

    private fun handleMutations(
        filteredMutations: Map<String, List<Mutation>>, preprocessedStorage: PreprocessStorage?
    ) {
        val executor = Executors.newFixedThreadPool(Configuration.currentConfig!!.numberOfThreads) as ThreadPoolExecutor
        var curCls = ""  // This curCls var is only used for info logging purpose
        if (filteredMutations.isEmpty()) logger.info("No mutations to run")
        filteredMutations.forEach label@{ (cls, mutationList) ->
            if (curCls != cls) {
                curCls = cls
                logger.infoVerbose("Mutation test for class $cls - with ${filteredMutations[cls]?.size} mutants")
            }
            executor.execute(
                Executor(
                    preprocessedStorage, buildRoot, cls, logger, mutationList,
                    fOpNames, mutationStorage, createdAgentLocation!!
                )
            )
        }
        executor.shutdown()
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        } catch (e: InterruptedException) {
            logger.error(e.toString())
        }
    }

    internal class Executor(
        private val preprocessedStorage: PreprocessStorage?,
        private val buildRoot: String,
        private val cls: String,
        private val logger: MoCoLogger,
        private val mutationList: List<Mutation>,
        private val fOpNames: List<String>,
        private val mutationStorage: MutationStorage,
        private val createdAgentLocation: String

    ) : Runnable {
        override fun run() {
            val className = ClassName(cls)
//            val testRetriever = RelatedTestRetriever(buildRoot)
            // Map from lines of code of the target class under test to test cases that cover them
            val lineTestsMapping = (preprocessedStorage?.classRecord?.find {
                it.classUnderTestName == className.name
            })?.coveredLines
            if (lineTestsMapping.isNullOrEmpty()) {
                logger.debug("Skip -- Line to tests mapping of ${className.getJavaName()} is unavailable")
                return
            }

            val temp = mutableListOf<Mutation>()
            for (i in mutationList.indices) {
                // Launch a new process for each class to handle the list of its mutants
                val processArgs =
                    WorkerProcess.processArgs(createdAgentLocation, Configuration.currentConfig!!.classPath)
                val workerProcess = WorkerProcess(
                    MutationTestWorker::class.java, processArgs,
                    listOf((processArgs["port"] as ServerSocket).localPort.toString())
                )
                temp.add(mutationList[i])
                val numberOfTestsToRun: Int = getNumberOfTestsToRun(temp, lineTestsMapping)
                if (numberOfTestsToRun >= 50) {
                    val workerThread = workerProcess.createMutationWorkerThread(
                        temp, lineTestsMapping, fOpNames, mutationStorage
                    )
                    workerProcess.execMutationTestProcess(workerThread)
                    temp.clear()
                } else {
                    if (i == mutationList.size - 1) {
                        val workerThread = workerProcess.createMutationWorkerThread(
                            temp, lineTestsMapping, fOpNames, mutationStorage
                        )
                        workerProcess.execMutationTestProcess(workerThread)
                    }
                }
            }
            return
        }

        private fun getNumberOfTestsToRun(mutationList: List<Mutation>,
                                          lineTestsMapping: MutableMap<Int, MutableSet<SerializableTestInfo>>): Int {
            var res = 0
            mutationList.map { res += lineTestsMapping[it.lineOfCode]?.size ?: 0 }
            return res
        }
    }

    private fun persistMutationResults(preprocessedStorage: PreprocessStorage) {
        val jsonSource = JsonSource("${mocoBuildPath}${File.separator}$mutationResultsFolder", "moco")
        var updatedMutationStorage = mutationStorage
        if (gitMode && gitProcessor != null) {
            logger.debug("Persistence of mutation test results...")
            // Mutants black list UPDATE - ADD (mutation results with status as run_error)
            MutantsBlackList().saveErrorMutants(mutationStorage)
            // Progress Class Test UPDATE - ADD (class progress - mutation results with status as survived and killed)
            PersistentMutationResult().saveMutationResult(mutationStorage, clsByGit, preprocessedStorage)
            updatedMutationStorage = updateWithExistingMutationResults(mutationStorage, jsonSource)
            ProgressClassTest().saveProgress(mutationStorage, fOpNames.joinToString(","))
        }
        if (Configuration.currentConfig!!.useForCICD) {
            logger.infoVerbose("Saved mutation test results to moco.json")
            jsonSource.save(updatedMutationStorage)
        } else logger.infoVerbose("Skip saving mutation results to json file because useForCICD is currently false")
    }

    private fun updateWithExistingMutationResults(
        additionalMutationStorage: MutationStorage,
        jsonSource: JsonSource
    ): MutationStorage {
        // returned storage contains all mutation results to be used in 3rd application
        val data = jsonSource.getData(MutationStorage::class.java)
        var existingMocoJSON: MutationStorage? = null
        if (data != null) existingMocoJSON = data as MutationStorage

        val updatedMocoJSON: MutationStorage
        if (existingMocoJSON != null && existingMocoJSON.runID == lastRunIDFromMeta) {
            // Everything is synchronized -> proceed by updating existing moco.json with new mutation results.
            existingMocoJSON.runID = additionalMutationStorage.runID
            updatedMocoJSON = existingMocoJSON
            logger.debug("Update mutation storage with existing mutation results from moco.json")
            additionalMutationStorage.entries.map {
                updatedMocoJSON.entries[it.key] = it.value
            }
        } else {
            // moco.json file can't be used -> use database
            logger.debug("Update mutation storage with existing mutation results from Database")
            val retrieveEntries = PersistentMutationResult().getAllData()
            updatedMocoJSON = MutationStorage(retrieveEntries, additionalMutationStorage.runID)
            additionalMutationStorage.entries.map {
                if (it.key in updatedMocoJSON.entries.keys) {
                    val temp = updatedMocoJSON.entries[it.key]!!
                    val temp1 = it.value.filter { it1 -> temp.any { it2 -> it2["uniqueID"] != it1["uniqueID"] } }
                    updatedMocoJSON.entries[it.key]?.addAll(temp1)
                } else {
                    updatedMocoJSON.entries[it.key] = it.value
                }
            }
        }
        return updatedMocoJSON
    }
}