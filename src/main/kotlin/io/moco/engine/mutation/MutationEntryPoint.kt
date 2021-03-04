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
    private val classPath: String,
    private val fOpNames: List<String> = Configuration.currentConfig!!.fOpNames,
    private val mocoBuildPath: String = Configuration.currentConfig!!.mocoBuildPath,
    private val mutationResultsFolder: String = Configuration.currentConfig!!.mutationResultsFolder,
    private val buildRoot: String = Configuration.currentConfig!!.buildRoot,
    private val gitMode: Boolean = Configuration.currentConfig!!.gitMode

) {

    val logger = MoCoLogger()

    fun mutationTest() {
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
        val foundMutations: MutableMap<ClassName, List<Mutation>> = mutableMapOf()
        for (item in preprocessedStorage.classRecord) {
            foundMutations[ClassName(item.classUnderTestName)] =
                mGen.findPossibleMutationsOfClass(item.classUnderTestName, item.coveredLines).distinct()
        }
        val filteredMutations = foundMutations.filter { it.value.isNotEmpty() }
        logger.debug("Found ${filteredMutations.size} class(es) can be mutated")
        logger.debug("Complete mutation collecting step")

        // Mutants generation and tests execution
        logger.debug("Start mutation testing")
        handleMutations(filteredMutations, preprocessedStorage)
        if (gitMode) {
            persistMutationResults()
        }
        logger.debug("Complete mutation testing")
    }

    private fun handleMutations(
        filteredMutations: Map<ClassName, List<Mutation>>, preprocessedStorage: PreprocessStorage?
    ) {
        filteredMutations.forEach label@{ (className, mutationList) ->
            val testRetriever = RelatedTestRetriever(buildRoot)
            val processArgs = WorkerProcess.getProcessArguments(createdAgentLocation, classPath)
            val relatedTestClasses: List<ClassName> = testRetriever.retrieveRelatedTest(
                className,
                preprocessedStorage?.testsExecutionTime!!
            )
            if (relatedTestClasses.isEmpty()) {
                logger.debug("Class ${className.getJavaName()} has 0 relevant test")
                return@label
            }
            logger.debug("Class ${className.getJavaName()} has ${relatedTestClasses.size} relevant tests")
            val workerProcess = WorkerProcess(
                MutationTestWorker::class.java, processArgs,
                listOf((processArgs["port"] as ServerSocket).localPort.toString())
            )
            val workerThread = workerProcess.createMutationWorkerThread(
                mutationList, relatedTestClasses, fOpNames, mutationStorage
            )
            workerProcess.execMutationTestProcess(workerThread)
        }
    }

    private fun persistMutationResults() {
        logger.debug("Persist mutation test results")
        JsonSource(
            "${mocoBuildPath}${File.separator}$mutationResultsFolder", "mutation"
        ).save(mutationStorage)
        val gh = gitProcessor.headCommit.name
        MutantsBlackList().saveErrorMutants(mutationStorage, gh)
        ProgressClassTest().saveProgress(
            mutationStorage, gh,
            fOpNames.joinToString(",")
        )
        PersistentMutationResult().saveMutationResult(mutationStorage, gh)
    }
}