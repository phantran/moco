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

import io.moco.utils.ByteArrayLoader
import io.moco.engine.mutation.*
import io.moco.engine.operator.Operator
import io.moco.engine.preprocessing.PreprocessStorage
import io.moco.engine.preprocessing.PreprocessingFilterByGit
import io.moco.utils.JsonConverter
import io.moco.engine.preprocessing.PreprocessorWorker
import io.moco.engine.test.RelatedTestRetriever
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
    private val filteredMutationOperatorNames: List<String>
    private val mutationStorage: MutationStorage = MutationStorage(mutableMapOf())
    private var filteredClsByGit: List<String>? = mutableListOf()

    init {
        logger.info("-----------------------------------------------------------------------")
        logger.info("                               M O C O")
        logger.info("-----------------------------------------------------------------------")
        logger.info("START")
        val cp = configuration.classPath.joinToString(separator = File.pathSeparatorChar.toString())
        classPath = "$cp:${configuration.codeRoot}:${configuration.testRoot}:${configuration.buildRoot}"
        byteArrLoader = ByteArrayLoader(cp)
        filteredMutationOperatorNames = Operator.supportedOperatorNames.filter {
            !configuration.excludedMutationOperatorNames.contains(it)
        }
        logger.info(
            "${filteredMutationOperatorNames.size} selected mutation operators: " +
                    filteredMutationOperatorNames.joinToString(", ")
        )
        MoCoLogger.verbose = configuration.verbose
    }

    fun execute() {
        val executionTime = measureTimeMillis {
            if (!initMoCoOK()) {
                logger.info("EXIT: Nothing to do")
                return
            }

            logger.info("Preprocessing started......")
            preprocessing()
            logger.info("Preprocessing completed")
            logger.info("Mutation Test started......")
            mutationTest()
            logger.info("Mutation Test completed")

            // Remove generated agent after finishing
            JarUtil.removeTemporaryAgentJar(createdAgentLocation)
        }
        logger.info("Execution done after ${executionTime / 1000}s")
        logger.info("DONE")
    }

    private fun initMoCoOK(): Boolean {
        if (filteredMutationOperatorNames.isEmpty()) {
            return false
        }
        if (configuration.gitMode) {
            logger.info("Git mode: on")
            filteredClsByGit = PreprocessingFilterByGit.getChangedClsSinceLastStoredCommit(
                configuration.artifactId.replace(".", "/"), configuration.baseDir
            )
            if (configuration.gitMode) {
                if (filteredClsByGit.isNullOrEmpty()) {
                    logger.info("Preprocessing: Git mode: No changed files found by git commits diff")
                    // skip preprocessing in git mode and no detected changed class
                    return false
                }
                logger.info("Preprocessing: Git mode - ${filteredClsByGit!!.size} changed classes by git commits diff")
                logger.debug("Classes found: $filteredClsByGit")
            }
        } else {
            logger.info("Git mode: off")
        }
        createdAgentLocation = JarUtil.createTemporaryAgentJar(byteArrLoader)
        if (createdAgentLocation == null) {
            logger.info("Error while creating MoCo Agent Jar")
            return false
        }

        // Clear preprocessing JSON if exists
        JsonConverter(
            "${configuration.buildRoot}/moco/preprocess/", configuration.preprocessResultFileName
        ).removeJSONFileIfExists()

        return true
    }

    private fun preprocessing() {
        var processStatus = MoCoProcessCode.NOT_STARTED.code
        var previousStatus = MoCoProcessCode.OK.code
        val processWorkerArguments = configuration.getPreprocessProcessArgs()
        processWorkerArguments.add(filteredClsByGit!!.joinToString(","))
        while (processStatus != MoCoProcessCode.OK.code) {
            val preprocessWorkerProcess = WorkerProcess(
                PreprocessorWorker.javaClass,
                getProcessArguments(),
                processWorkerArguments
            )
            preprocessWorkerProcess.start()
            processStatus = preprocessWorkerProcess.getProcess()?.waitFor()!!
            if (processStatus ==  MoCoProcessCode.UNRECOVERABLE_ERROR.code ||
                previousStatus == processStatus) {
                break
            } else {
                previousStatus = processStatus
                // Add more parameter to param to process to signal that this is a rerun because of previous error
                if (processWorkerArguments.getOrNull(13) == null) processWorkerArguments.add("true")
            }
        }
        if (processStatus != MoCoProcessCode.OK.code) {
            logger.warn("Please check you test suite because there might be erroneous tests in your test suite")
        }
    }

    private fun mutationTest() {
        val preprocessedStorage = PreprocessStorage.getStoredPreprocessStorage(configuration.buildRoot)
        if (preprocessedStorage == null) {
            logger.info("No preprocess results, skip mutation testing")
            return
        }
        if (preprocessedStorage.classRecord.isNullOrEmpty() || preprocessedStorage.testsExecutionTime.isNullOrEmpty()) {
            logger.info("No changed classes detected, skip mutation testing")
            return
        }
        // Mutations collecting
        logger.debug("Start mutation collecting")

        val mGen =
            MutationGenerator(byteArrLoader, filteredMutationOperatorNames.mapNotNull { Operator.nameToOperator(it) })
        val foundMutations: MutableMap<ClassName, List<Mutation>> = mutableMapOf()
        for (item in preprocessedStorage.classRecord) {
            foundMutations[ClassName(item.classUnderTestName)] =
                mGen.findPossibleMutationsOfClass(item.classUnderTestName, item.coveredLines).distinct()
        }
        val filteredFoundMutations = foundMutations.filter { it.value.isNotEmpty() }
        logger.debug("Found ${filteredFoundMutations.size} class(es) can be mutated")
        logger.debug("Complete mutation collecting step")

        // Mutants generation and tests execution
        val testRetriever = RelatedTestRetriever(configuration.buildRoot)
        logger.debug("Start mutation testing")
        filteredFoundMutations.forEach label@{ (className, mutationList) ->
            val processArgs = getProcessArguments()
            val relatedTestClasses: List<ClassName> = testRetriever.retrieveRelatedTest(
                className,
                preprocessedStorage.testsExecutionTime!!
            )
            if (relatedTestClasses.isEmpty()) {
                logger.debug("Class ${className.getJavaName()} has 0 relevant test")
                return@label
            }
            logger.debug("Class ${className.getJavaName()} has ${relatedTestClasses.size} relevant tests")
            val mutationTestWorkerProcess =
                createMutationTestWorkerProcess(mutationList, relatedTestClasses, processArgs)
            executeMutationTestingProcess(mutationTestWorkerProcess)
        }
        JsonConverter(
            "${configuration.buildRoot}/moco/mutation/",
            configuration.mutationResultsFileName
        ).saveMutationResultsToJson(mutationStorage)
        logger.debug("Complete mutation testing")

    }

    private fun executeMutationTestingProcess(p: Pair<WorkerProcess, ResultsReceiverThread>) {
        val process = p.first
        val comThread = p.second
        process.start()
        comThread.start()
        try {
            comThread.waitUntilFinish()
        } finally {
            process.destroyProcess()
        }
    }

    private fun createMutationTestWorkerProcess(
        mutations: List<Mutation>, tests: List<ClassName>,
        processArgs: MutableMap<String, Any>
    ): Pair<WorkerProcess, ResultsReceiverThread> {

        val mutationWorkerArgs =
            ResultsReceiverThread.MutationWorkerArguments(
                mutations,
                tests,
                classPath,
                filteredMutationOperatorNames,
                "",
                configuration.testTimeOut,
                configuration.debugEnabled,
                configuration.verbose
            )
        val mutationTestWorkerProcess = WorkerProcess(
            MutationTestWorker::class.java,
            processArgs,
            listOf((processArgs["port"] as ServerSocket).localPort.toString())
        )
        val comThread = ResultsReceiverThread(processArgs["port"] as ServerSocket, mutationWorkerArgs, mutationStorage)
        return Pair(mutationTestWorkerProcess, comThread)
    }

    private fun getProcessArguments(): MutableMap<String, Any> {
        return mutableMapOf(
            "port" to ServerSocket(0), "javaExecutable" to configuration.jvm,
            "javaAgentJarPath" to "-javaagent:$createdAgentLocation", "classPath" to classPath
        )
    }
}