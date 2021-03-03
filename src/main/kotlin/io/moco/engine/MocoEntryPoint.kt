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

package io.moco.engine

import io.moco.engine.Metrics.calculateAccumulatedCoverage
import io.moco.engine.Metrics.calculateRunCoverage
import io.moco.utils.ByteArrayLoader
import io.moco.engine.mutation.*
import io.moco.engine.operator.Operator
import io.moco.engine.preprocessing.PreprocessStorage
import io.moco.utils.GitProcessor
import io.moco.engine.preprocessing.PreprocessorWorker
import io.moco.engine.test.RelatedTestRetriever
import io.moco.persistence.*
import io.moco.utils.JarUtil
import java.io.File
import java.net.ServerSocket

import io.moco.utils.MoCoLogger
import kotlin.system.measureTimeMillis


class MocoEntryPoint(private val configuration: Configuration) {
    // Preprocessing step: Parse the targets source code and tests to collect information
    // about code blocks and mapping from classes under test to test classes
    // (which test classes are responsible for which classes under test)
    private val logger = MoCoLogger()
    private val classPath: String
    private var byteArrLoader: ByteArrayLoader
    private var createdAgentLocation: String? = null
    private val filteredMuOpNames: List<String>
    private val mutationStorage: MutationStorage = MutationStorage(mutableMapOf())
    private var filteredClsByGit: List<String>? = mutableListOf()
    private var projectMeta: ProjectMeta? = null
    private var recordedTestMapping: String? = null
    private var gitProcessor = GitProcessor(configuration.baseDir)

    init {
        logger.info("-----------------------------------------------------------------------")
        logger.info("                               M O C O")
        logger.info("-----------------------------------------------------------------------")
        logger.info("START")
        projectMeta = ProjectMeta()
        val cp = configuration.classPath.joinToString(separator = File.pathSeparatorChar.toString())
        classPath = "$cp:${configuration.codeRoot}:${configuration.testRoot}:${configuration.buildRoot}"
        byteArrLoader = ByteArrayLoader(cp)
        filteredMuOpNames = Operator.supportedOperatorNames.filter {
            !configuration.excludedMuOpNames.contains(it)
        }
        logger.info(
            "${filteredMuOpNames.size} selected mutation operators: " +
                    filteredMuOpNames.joinToString(", ")
        )
        MoCoLogger.verbose = configuration.verbose
    }

    fun execute() {
        val executionTime = measureTimeMillis {
            if (!initMoCoOK()) {
                logger.info("EXIT: Nothing to do")
            } else {
                logger.info("Preprocessing started......")
                preprocessing()
                logger.info("Preprocessing completed")
                logger.info("Mutation Test started......")
                mutationTest()
                logger.info("Mutation Test completed")
            }

        }
        logger.info("Execution done after ${executionTime / 1000}s")
        reportResults()
        cleanBeforeExit()
        logger.info("DONE")
    }

    private fun reportResults() {
        val runCoverage = calculateRunCoverage(mutationStorage)
        val accumulatedCoverage = calculateAccumulatedCoverage(filteredMuOpNames.joinToString("','"))
        logger.info("-----------------------------------------------------------------------")
        logger.info("Mutation Coverage of this run: $runCoverage")
        logger.info("Accumulated Coverage for current configuration: $accumulatedCoverage")
        logger.info("-----------------------------------------------------------------------")

        // persist this run to run history
        logger.debug("Saving new entry to project history")
        val temp = ProjectTestHistory()
        temp.entry = mutableMapOf(
            "commit_id" to gitProcessor.headCommit.name,
            "branch" to gitProcessor.branch, "run_operators" to filteredMuOpNames.joinToString(","),
            "run_coverage" to runCoverage.toString(), "accumulated_coverage" to accumulatedCoverage.toString(),
            "git_mode" to configuration.gitMode.toString()
        )
        temp.save()
    }

    private fun cleanBeforeExit() {
        // Save meta before exit
        logger.debug("Saving project meta data before exiting...")
        if (configuration.gitMode) {
            gitProcessor.setHeadCommitMeta(projectMeta!!)
            projectMeta!!.meta["sourceBuildFolder"] = configuration.codeRoot
            projectMeta!!.meta["testBuildFolder"] = configuration.testRoot
            projectMeta?.saveMetaData()
        }
        // Remove generated agent after finishing
        JarUtil.removeTemporaryAgentJar(createdAgentLocation)
        logger.debug("Remove temporary moco agent jar successfully")
    }

    private fun initMoCoOK(): Boolean {
        if (filteredMuOpNames.isEmpty()) {
            return false
        }

        val gitOK = gitInfoProcessing()
        if (!gitOK) return false

        createdAgentLocation = JarUtil.createTemporaryAgentJar(byteArrLoader)
        if (createdAgentLocation == null) {
            logger.info("Error while creating MoCo Agent Jar")
            return false
        }
        // Clear preprocessing JSON if exists
        JsonSource(
            "${configuration.mocoBuildPath}${File.separator}${configuration.preprocessResultsFolder}",
            "preprocess"
        ).removeJSONFileIfExists()
        return true
    }

    private fun gitInfoProcessing(): Boolean {
        if (configuration.gitMode) {
            logger.info("Git mode: on")
            logger.info("Latest stored commit: ${projectMeta?.meta?.get("latestStoredCommitID")}")

            if (projectMeta?.meta?.get("latestStoredCommitID").isNullOrEmpty()) {
                logger.info("Last commit info does not exist - skip Git commits diff analysis - proceed in normal mode")
            } else {
                filteredClsByGit = gitProcessor.getChangedClsSinceLastStoredCommit(
                    configuration.artifactId.replace(".", "/"), projectMeta?.meta!!
                )
                if (filteredClsByGit != null) {
                    if (filteredClsByGit?.isEmpty() == true) {
                        logger.info("Preprocessing: Git mode: No changed files found by git commits diff")
                        // skip preprocessing in git mode and no detected changed class
                        return false
                    }
                    logger.info("Preprocessing: Git mode - ${filteredClsByGit!!.size} changed classes by git commits diff")
                    logger.debug("Classes found: $filteredClsByGit")
                    recordedTestMapping = projectMeta?.meta!!["latestStoredCommitID"]?.let {
                        TestsCutMapping().getRecordedMapping(
                            filteredClsByGit!!,
                            it
                        )
                    }
                } else {
                    filteredClsByGit = listOf("")
                    logger.info("Preprocessing: Git mode: last stored commit not found - proceed in normal mode")
                }
            }
        } else {
            logger.info("Git mode: off")
        }
        return true
    }

    private fun preprocessing() {
        var processStatus = MoCoProcessCode.NOT_STARTED.code
        var previousStatus = MoCoProcessCode.OK.code
        val processWorkerArguments = configuration.getPreprocessProcessArgs()
        processWorkerArguments.add(filteredClsByGit!!.joinToString(","))
        if (recordedTestMapping != null) processWorkerArguments.add(recordedTestMapping!!)
        else processWorkerArguments.add("")
        while (processStatus != MoCoProcessCode.OK.code) {
            val preprocessWorkerProcess = WorkerProcess(
                PreprocessorWorker.javaClass, getProcessArguments(),
                processWorkerArguments
            )
            preprocessWorkerProcess.start()
            processStatus = preprocessWorkerProcess.getProcess()?.waitFor()!!
            if (processStatus == MoCoProcessCode.UNRECOVERABLE_ERROR.code ||
                previousStatus == processStatus
            ) break
            else {
                previousStatus = processStatus
                // Add more parameter to param to process to signal that this is a rerun because of previous error
                if (processWorkerArguments.getOrNull(14) == null) processWorkerArguments.add("true")
            }
        }
        if (processStatus != MoCoProcessCode.OK.code) {
            logger.warn("Please check you test suite because there might be erroneous tests in your test suite")
        }
    }

    private fun mutationTest() {
        val preprocessedStorage = PreprocessStorage.getStoredPreprocessStorage(configuration.mocoBuildPath)
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

        val mGen =
            MutationGenerator(byteArrLoader, filteredMuOpNames.mapNotNull { Operator.nameToOperator(it) })
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
        persistMutationResults()
        logger.debug("Complete mutation testing")
    }

    private fun handleMutations(
        filteredMutations: Map<ClassName, List<Mutation>>, preprocessedStorage: PreprocessStorage?
    ) {
        filteredMutations.forEach label@{ (className, mutationList) ->
            val testRetriever = RelatedTestRetriever(configuration.buildRoot)
            val processArgs = getProcessArguments()
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
            val workerThread = createMutationWorkerThread(mutationList, relatedTestClasses, processArgs)
            workerProcess.execMutationTestProcess(workerThread)
        }
    }

    private fun createMutationWorkerThread(
        mutations: List<Mutation>, tests: List<ClassName>, processArgs: MutableMap<String, Any>
    ): ResultsReceiverThread {
        val mutationWorkerArgs =
            ResultsReceiverThread.MutationWorkerArguments(
                mutations, tests, classPath, filteredMuOpNames,
                "", configuration.testTimeOut, configuration.debugEnabled, configuration.verbose
            )
        return ResultsReceiverThread(processArgs["port"] as ServerSocket, mutationWorkerArgs, mutationStorage)
    }

    private fun getProcessArguments(): MutableMap<String, Any> {
        return mutableMapOf(
            "port" to ServerSocket(0), "javaExecutable" to configuration.jvm,
            "javaAgentJarPath" to "-javaagent:$createdAgentLocation", "classPath" to classPath
        )
    }

    private fun persistMutationResults() {
        logger.debug("Persist mutation test results")
        JsonSource(
            "${configuration.mocoBuildPath}${File.separator}${configuration.mutationResultsFolder}",
            "mutation"
        ).save(mutationStorage)
        PersistentMutationResult().saveMutationResult(mutationStorage, gitProcessor.headCommit.name)
    }
}