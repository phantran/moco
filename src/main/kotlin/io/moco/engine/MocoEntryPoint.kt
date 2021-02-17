package io.moco.engine

import io.moco.engine.io.ByteArrayLoader
import io.moco.engine.mutation.*
import io.moco.engine.operator.Operator
import io.moco.engine.preprocessing.PreprocessStorage
import io.moco.utils.JsonConverter
import io.moco.engine.preprocessing.PreprocessorWorker
import io.moco.engine.test.RelatedTestRetriever
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ThreadPoolExecutor

import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest


class MocoEntryPoint {
    // Preprocessing step: Parse the targets source code and tests to collect information
    // about code blocks and mapping from classes under test to test classes
    // (which test classes are responsible for which classes under test)
    private val codeRoot: String = Configuration.codeRoot
    private val testRoot: String = Configuration.testRoot
    private val buildRoot = Configuration.buildRoot
    private val jvm: String = Configuration.jvm

    private val excludedClasses: String = Configuration.excludedClasses
    private val classPath: String
    private var byteArrLoader: ByteArrayLoader
    private var createdAgentLocation: String?
    private val filteredMutationOperator: List<Operator>
    private val mutationStorage: MutationStorage = MutationStorage(mutableMapOf())

    init {
        val cp = Configuration.classPath.joinToString(separator = File.pathSeparatorChar.toString())
        classPath = "$cp:$codeRoot:$testRoot:$buildRoot"
        byteArrLoader = ByteArrayLoader(cp)
        createdAgentLocation = createTemporaryAgentJar()
        filteredMutationOperator =
            Operator.supportedOperatorNames.filter { !Configuration.excludedMutationOperatorNames.contains(it) }
                .mapNotNull { Operator.nameToOperator(it) }
    }

    fun execute() {
        if (createdAgentLocation == null) {
            return
        }
//        val executor = ThreadPoolExecutor(
//            1, 2,
//            10, TimeUnit.SECONDS, LinkedBlockingQueue(),
//            Executors.defaultThreadFactory()
//        )

        // Preprocessing step
        preprocessing()
        mutationTest()


        // Remove generated agent after finishing
        removeTemporaryAgentJar(createdAgentLocation)
    }

    private fun preprocessing() {
        val workerArgs = mutableListOf(codeRoot, testRoot, excludedClasses, buildRoot, Configuration.preprocessFilename)
        val preprocessWorkerProcess = WorkerProcess(
            PreprocessorWorker.javaClass,
            getPreprocessWorkerArgs(),
            workerArgs
        )
        preprocessWorkerProcess.start()
    }

    private fun mutationTest() {
        // Mutations collecting
        val preprocessedStorage = PreprocessStorage.getStoredPreprocessStorage(buildRoot)
        val toBeMutatedClasses: List<ClassName> = preprocessedStorage.classRecord.map { ClassName(it.classUnderTestName) }
        val mGen = MutationGenerator(byteArrLoader, filteredMutationOperator)
        var foundMutations: Map<ClassName, List<Mutation>> =
            toBeMutatedClasses.associateWith { mGen.findPossibleMutationsOfClass(it) }
        foundMutations = foundMutations.filter { it.value.isNotEmpty() }

        // Mutants generation and tests execution
        val testRetriever = RelatedTestRetriever(buildRoot)
        val processArgs = getMutationPreprocessArgs()
        foundMutations.forEach label@{ (className, mutationList) ->
            println("Starting executing tests for mutants of class $className")
            val relatedTests: List<ClassName> = testRetriever.retrieveRelatedTest(className)
            if (relatedTests.isEmpty()) {
                return@label
            }
            val mutationTestWorkerProcess = createMutationTestWorkerProcess(mutationList, relatedTests, processArgs)
            executeMutationTestingProcess(mutationTestWorkerProcess)
        }
        JsonConverter(
            "$buildRoot/moco/mutation/",
            Configuration.mutationResultsFilename
        ).saveObjectToJson(mutationStorage)
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
            ResultsReceiverThread.MutationWorkerArguments(mutations, tests, classPath, filteredMutationOperator, "")
        val mutationTestWorkerProcess = WorkerProcess(
            MutationTestWorker::class.java,
            processArgs,
            listOf((processArgs["port"] as ServerSocket).localPort.toString())
        )
        val comThread = ResultsReceiverThread(processArgs["port"] as ServerSocket, mutationWorkerArgs, mutationStorage)
        return Pair(mutationTestWorkerProcess, comThread)
    }

    private fun getMutationPreprocessArgs(): MutableMap<String, Any> {
        return getPreprocessWorkerArgs()
    }

    private fun getPreprocessWorkerArgs(): MutableMap<String, Any> {
        return mutableMapOf(
            "port" to ServerSocket(0), "javaExecutable" to jvm,
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