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

import io.moco.engine.io.ByteArrayLoader
import io.moco.engine.mutation.*
import io.moco.engine.operator.Operator
import io.moco.engine.preprocessing.PreprocessStorage
import io.moco.engine.preprocessing.PreprocessingFilterByGit
import io.moco.utils.JsonConverter
import io.moco.engine.preprocessing.PreprocessorWorker
import io.moco.engine.test.RelatedTestRetriever
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ServerSocket

import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import io.moco.utils.MoCoLogger
import kotlin.system.measureTimeMillis


class MocoEntryPoint(private val configuration: Configuration) {
    // Preprocessing step: Parse the targets source code and tests to collect information
    // about code blocks and mapping from classes under test to test classes
    // (which test classes are responsible for which classes under test)
    private val logger = MoCoLogger()
    private val classPath: String
    private var byteArrLoader: ByteArrayLoader
    private var createdAgentLocation: String?
    private val filteredMutationOperatorNames: List<String>
    private val mutationStorage: MutationStorage = MutationStorage(mutableMapOf())
    private var filteredClsByGit: List<String>? = mutableListOf()

    init {
        val cp = configuration.classPath.joinToString(separator = File.pathSeparatorChar.toString())
        classPath = "$cp:${configuration.codeRoot}:${configuration.testRoot}:${configuration.buildRoot}"
        byteArrLoader = ByteArrayLoader(cp)
        createdAgentLocation = createTemporaryAgentJar()
        filteredMutationOperatorNames = Operator.supportedOperatorNames.filter {
            !configuration.excludedMutationOperatorNames.contains(it)
        }
        if (configuration.gitChangedClassesMode) {
            filteredClsByGit = PreprocessingFilterByGit.getChangedClsSinceLastStoredCommit(
                configuration.artifactId.replace(".", "/"), configuration.baseDir
            )
        }
    }

    fun execute() {
        val executionTime = measureTimeMillis {
            if (createdAgentLocation == null) {
                return
            }
            logger.info("START")
            // Skip preprocessing if no detected changed classes in git based mode
            logger.info("Preprocessing started...")
            // preprocessing()
            logger.info("Preprocessing completed")

            logger.info("Mutation Test started...")
            mutationTest()
            logger.info("Mutation Test completed")

            // Remove generated agent after finishing
            removeTemporaryAgentJar(createdAgentLocation)
        }
        logger.info("Execution done after ${executionTime/1000}s" )
        logger.info("DONE")
    }

    private fun preprocessing() {
        if (configuration.gitChangedClassesMode) {
            if (filteredClsByGit.isNullOrEmpty()) {
                logger.info("Preprocessing: Git mode: No changed files found by git commits diff")
                // skip preprocessing in git mode and no detected changed class
                return
            }
            logger.info("Preprocessing: Git mode - ${filteredClsByGit!!.size} changed classes by git commits diff")
            logger.debug("Classes found: $filteredClsByGit" )
        }
        val processWorkerArguments = configuration.getPreprocessProcessArgs()
        processWorkerArguments.add(filteredClsByGit!!.joinToString(","))
        val preprocessWorkerProcess = WorkerProcess(
            PreprocessorWorker.javaClass,
            getProcessArguments(),
            processWorkerArguments
        )
        preprocessWorkerProcess.start()
        preprocessWorkerProcess.getProcess()?.waitFor()
    }

    private fun mutationTest() {
        // Mutations collecting
        logger.debug("Start mutation collecting")

        val preprocessedStorage = PreprocessStorage.getStoredPreprocessStorage(configuration.buildRoot)
        if (preprocessedStorage.classRecord.isNullOrEmpty()) {
            logger.info("No new ")
            return
        }
        val toBeMutatedClasses: List<ClassName> =
            preprocessedStorage.classRecord.map { ClassName(it.classUnderTestName) }
        val mGen =
            MutationGenerator(byteArrLoader, filteredMutationOperatorNames.mapNotNull { Operator.nameToOperator(it) })
        var foundMutations: Map<ClassName, List<Mutation>> =
            toBeMutatedClasses.associateWith { mGen.findPossibleMutationsOfClass(it) }
        foundMutations = foundMutations.filter { it.value.isNotEmpty() }
        logger.debug("Complete mutation collecting step")

        // Mutants generation and tests execution
        val testRetriever = RelatedTestRetriever(configuration.buildRoot)
        logger.debug("Start mutation testing")
        foundMutations.forEach label@{ (className, mutationList) ->
            val processArgs = getMutationPreprocessArgs()
            val relatedTests: List<ClassName> = testRetriever.retrieveRelatedTest(className)
            if (relatedTests.isEmpty()) {
                return@label
            }
            val mutationTestWorkerProcess = createMutationTestWorkerProcess(mutationList, relatedTests, processArgs)
            executeMutationTestingProcess(mutationTestWorkerProcess)
        }
        JsonConverter(
            "${configuration.buildRoot}/moco/mutation/",
            configuration.mutationResultsFileName
        ).saveObjectToJson(mutationStorage)
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
        mutations: List<Mutation>,
        tests: List<ClassName>,
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
                MoCoLogger.debugEnabled
            )
        val mutationTestWorkerProcess = WorkerProcess(
            MutationTestWorker::class.java,
            processArgs,
            listOf((processArgs["port"] as ServerSocket).localPort.toString())
        )
        val comThread = ResultsReceiverThread(processArgs["port"] as ServerSocket, mutationWorkerArgs, mutationStorage)
        return Pair(mutationTestWorkerProcess, comThread)
    }

    private fun getMutationPreprocessArgs(): MutableMap<String, Any> {
        return getProcessArguments()
    }

    private fun getProcessArguments(): MutableMap<String, Any> {
        return mutableMapOf(
            "port" to ServerSocket(0), "javaExecutable" to configuration.jvm,
            "javaAgentJarPath" to "-javaagent:$createdAgentLocation", "classPath" to classPath
        )
    }

    @Throws(IOException::class)
    private fun createTemporaryAgentJar(): String? {
        return try {
            val jarName = File.createTempFile(
                System.currentTimeMillis()
                    .toString() + ("" + Math.random()).replace("\\.".toRegex(), ""),
                ".jar"
            )

            val outputStream = FileOutputStream(jarName)
            val jarFile = File(jarName.absolutePath)
            val manifest = Manifest()
            manifest.clear()
            val temp = manifest.mainAttributes
            if (temp.getValue(Attributes.Name.MANIFEST_VERSION) == null) {
                temp[Attributes.Name.MANIFEST_VERSION] = "1.0"
            }
            temp.putValue(
                "Boot-Class-Path",
                jarFile.absolutePath.replace('\\', '/')
            )
            temp.putValue(
                "Agent-Class",
                MocoAgent::class.java.name
            )
            temp.putValue("Can-Redefine-Classes", "true")
            temp.putValue("Can-Retransform-Classes", "true")
            temp.putValue(
                "Premain-Class",
                MocoAgent::class.java.name
            )
            temp.putValue("Can-Set-Native-Method-Prefix", "true")
            JarOutputStream(outputStream, manifest).use {}

            jarName.absolutePath
        } catch (e: IOException) {
            throw e
        }
    }

    private fun removeTemporaryAgentJar(location: String?) {
        if (location != null) {
            val f = File(location)
            f.delete()
        }
    }

//    @Throws(IOException::class)
//    private fun addClass(clazz: Class<*>, jos: JarOutputStream) {
//        val className = clazz.name
//        val ze = ZipEntry(className.replace(".", "/") + ".class")
//        jos.putNextEntry(ze)
//        val temp = byteArrLoader.getByteCodeArray(className)
//        if (temp != null) {
//            jos.write(temp)
//        }
//        jos.closeEntry()
//    }
}