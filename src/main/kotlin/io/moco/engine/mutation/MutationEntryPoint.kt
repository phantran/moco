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
import io.moco.engine.test.RelatedTestRetriever
import io.moco.persistence.*
import io.moco.utils.ByteArrayLoader
import io.moco.utils.GitProcessor
import io.moco.utils.MoCoLogger
import java.io.File
import java.net.ServerSocket


class MutationEntryPoint(
    private val byteArrLoader: ByteArrayLoader,
    private val mutationStorage: MutationStorage,
    private val gitProcessor: GitProcessor,
    private val createdAgentLocation: String?,
    private val clsByGit: List<String>?,
    private val fOpNames: List<String> = Configuration.currentConfig!!.fOpNames,
    private val mocoBuildPath: String = Configuration.currentConfig!!.mocoBuildPath,
    private val mutationResultsFolder: String = Configuration.currentConfig!!.mutationResultsFolder,
    private val buildRoot: String = Configuration.currentConfig!!.buildRoot,
    private val gitMode: Boolean = Configuration.currentConfig!!.gitMode

) {
    val logger = MoCoLogger()

    fun mutationTest(newOperatorsSelected: Boolean) {
        val preprocessedStorage = PreprocessStorage.getPreprocessStorage(mocoBuildPath)
        if (preprocessedStorage == null) {
            logger.info("No preprocess results, skip mutation testing")
            return
        }
        if (preprocessedStorage.classRecord.isNullOrEmpty() || preprocessedStorage.testsExecutionTime.isNullOrEmpty()) {
            logger.info("No preprocess information available, skip mutation testing")
            return
        }
        // persist preprocess result to database
        TestsCutMapping().saveMappingInfo(preprocessedStorage.classRecord, gitProcessor.headCommit.name)
        // Mutations collecting
        logger.debug("Start mutation collecting")

        val mGen = MutationGenerator(byteArrLoader, fOpNames.mapNotNull { Operator.nameToOperator(it) })
        val foundMutations: MutableMap<String, List<Mutation>> = mutableMapOf()
        for (item in preprocessedStorage.classRecord) {
            foundMutations[item.classUnderTestName] =
                mGen.findPossibleMutationsOfClass(item.classUnderTestName, item.coveredLines).distinct()
        }
        val filteredMutations = filterMutations(foundMutations, newOperatorsSelected)
        logger.debug("Found ${filteredMutations.size} class(es) can be mutated")
        logger.debug("Complete mutation collecting step")

        // Mutants generation and tests execution
        logger.debug("Start mutation testing")
        handleMutations(filteredMutations, preprocessedStorage)

        persistMutationResults()

        logger.debug("Complete mutation testing")
    }

    private fun filterMutations(
        mutations: MutableMap<String, List<Mutation>>,
        newOperatorsSelected: Boolean
    ): Map<String, List<Mutation>> {
        if (gitMode && !newOperatorsSelected) {
            if (!clsByGit.isNullOrEmpty()) {
                // Mutants black list UPDATE - REMOVAL
                // remove black listed mutants of the changed classes by Git because their contents have been changed
                MutantsBlackList().removeData("class_name IN (\'${clsByGit.joinToString("\',\'")}\')")
            }
            val mutationBlackList = MutantsBlackList().getData("")
            for (item in mutationBlackList) {
                val bLEntry = item.entry
                if (mutations.keys.any { it == bLEntry["class_name"] }) {
                    // Mutants black list USAGE
                    val mList = mutations[bLEntry["class_name"]]?.toMutableList()
                    for (m in mList!!) {
                        if (m.lineOfCode.toString() == bLEntry["line_of_code"]
                            && m.mutationID.instructionIndices!!.joinToString(",") == bLEntry["instruction_indices"]
                            && m.mutationID.mutatorUniqueID == bLEntry["mutator_id"]
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

        val chunkedMutationsList: List<Pair<String, List<Mutation>>> = mutationsSplitting(filteredMutations)
        var curCls = ""  // This curCls var is only used for info logging purpose
        chunkedMutationsList.forEach label@{ (cls, mutationList) ->
            val className = ClassName(cls)
            val testRetriever = RelatedTestRetriever(buildRoot)
            val testsExecutionTime = preprocessedStorage?.testsExecutionTime!!
            val relatedTestClasses: List<ClassName> = testRetriever.retrieveRelatedTest(
                className,
                testsExecutionTime
            )
            if (relatedTestClasses.isEmpty()) {
                logger.debug("Class ${className.getJavaName()} has 0 relevant test")
                return@label
            }
            if (curCls != cls) {
                curCls = cls
                logger.info("Mutation test for class ${className.getJavaName()} - with ${filteredMutations[cls]?.size} mutants")
            }
            logger.debug("Class ${className.getJavaName()} has ${relatedTestClasses.size} relevant tests")

            val temp = mutableListOf<Mutation>()
            for (i in mutationList.indices) {
                // Launch a new process for each class to handle the list of its mutants
                val processArgs =
                    WorkerProcess.processArgs(createdAgentLocation, Configuration.currentConfig!!.classPath)
                val workerProcess = WorkerProcess(
                    MutationTestWorker::class.java, processArgs,
                    listOf((processArgs["port"] as ServerSocket).localPort.toString())
                )

                if (relatedTestClasses.size > 10) {
                    val workerThread = workerProcess.createMutationWorkerThread(
                        listOf(mutationList[i]), relatedTestClasses, testsExecutionTime, fOpNames, mutationStorage
                    )
                    workerProcess.execMutationTestProcess(workerThread)
                } else {
                    temp.add(mutationList[i])
                    if (temp.size * relatedTestClasses.size > 10) {
                        val workerThread = workerProcess.createMutationWorkerThread(
                            temp, relatedTestClasses, testsExecutionTime, fOpNames, mutationStorage
                        )
                        workerProcess.execMutationTestProcess(workerThread)
                        temp.clear()
                    } else if (i == mutationList.size - 1) {
                        val workerThread = workerProcess.createMutationWorkerThread(
                            temp, relatedTestClasses, testsExecutionTime, fOpNames, mutationStorage
                        )
                        workerProcess.execMutationTestProcess(workerThread)
                    }
                }
            }
        }
    }

    private fun mutationsSplitting(mutationsMap: Map<String, List<Mutation>>): MutableList<Pair<String, List<Mutation>>> {
        // Split 10 tests per process to make sure a process launch does not crash because of memory issue
        // when there are too many timeout tests
        val res: MutableList<Pair<String, List<Mutation>>> = mutableListOf()
        for ((clsName, ml) in mutationsMap) {
            if (ml.size > 10) {
                val temp = ml.chunked(10)
                temp.forEach { res.add(Pair(clsName, it)) }
            } else {
                res.add(Pair(clsName, ml))
            }
        }
        return res
    }

    private fun persistMutationResults() {
        logger.debug("Persist mutation test results")
        JsonSource("${mocoBuildPath}${File.separator}$mutationResultsFolder", "mutation")
            .save(mutationStorage)
        if (gitMode) {
            val gh = gitProcessor.headCommit.name
            // Mutants black list UPDATE - ADD (mutation results with status as run_error)
            MutantsBlackList().saveErrorMutants(mutationStorage)
            // Progress Class Test UPDATE - ADD (class progress - mutation results with status as survived and killed)
            ProgressClassTest().saveProgress(mutationStorage, fOpNames.joinToString(","))
            PersistentMutationResult().saveMutationResult(mutationStorage, gh)
        }
    }
}