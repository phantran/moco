package io.moco.engine

import io.moco.engine.io.BytecodeLoader
import io.moco.engine.mutation.Mutation
import io.moco.engine.mutation.MutationFinder
import io.moco.engine.mutation.MutationGenerator
import io.moco.engine.operator.Operator
import io.moco.engine.preprocessing.PreprocessorTracker
import io.moco.engine.preprocessing.PreprocessorWorker
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.nio.file.Paths

import java.nio.file.Path
import java.util.*
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry


class MocoEntryPoint(
    private val codeRoot: String,
    private val testRoot: String,
    private val excludedClasses: String,
    private val buildRoot: String,
    runtimeClassPath: MutableList<String>,
    private val compileClassPath: MutableList<String>

) {
    // Preprocessing step: Parse the targets source code and tests to collect information
    // about code blocks and mapping from classes under test to test classes
    // (which test classes are responsible for which classes under test)

    private var classPath: String
    private var clsLoader: BytecodeLoader

    init {
        val cp = runtimeClassPath.joinToString(separator = File.pathSeparatorChar.toString())
        classPath = "$cp:$codeRoot:$testRoot:$buildRoot"
        clsLoader = BytecodeLoader(cp)

    }

    fun execute() {
        //TODO: replace later
        val createdAgent: String = createJar() ?: return

        //TODO: replace included operators by params from mojo configuration
        val temp = listOf("AOR", "LCR", "ROR", "UOI")


        // Preprocessing step
        val workerArgs = mutableListOf(codeRoot, testRoot, excludedClasses, buildRoot)
        val workerProcess = WorkerProcess(
            PreprocessorWorker.javaClass,
            getPreprocessWorkerArgs(classPath, createdAgent), workerArgs
        )
        workerProcess.start()

        // Mutation step
        // Mutations collecting
        val toBeMutatedCodeBase = Codebase(codeRoot, testRoot, excludedClasses)
        val includedMutationOperators: List<Operator> = temp.mapNotNull { Operator.nameToOperator(it) }
        val mutationFinder = MutationFinder(clsLoader, includedMutationOperators)
        val mGen = MutationGenerator(toBeMutatedCodeBase, mutationFinder)
        val foundMutations: Map<ClassName, List<Mutation>> = mGen.codeBaseMutationAnalyze()

        // Mutation test generating and executing
//        val relatedTests: List<TestItemWrapper> = MutationFinder.retriveRelatedTest()


        // Remove generated agent when finish
        removeAgent(createdAgent)
    }

    private fun getPreprocessWorkerArgs(cp: Any, createdAgent: String): MutableMap<String, Any> {
        // TODO: will be replaced by configuration from Maven configuration parameters
        val preprocessWorkerArgs: MutableMap<String, Any> = mutableMapOf()
        preprocessWorkerArgs["port"] = ServerSocket(0)
        val javaHome = System.getProperty("java.home")
        val javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java"
        preprocessWorkerArgs["javaExecutable"] = javaBin
        val agentArg = "-javaagent:$createdAgent"
        preprocessWorkerArgs["javaAgentJarPath"] = agentArg
        preprocessWorkerArgs["classPath"] = cp
        return preprocessWorkerArgs
    }


    fun createJar(): String? {
        return try {
            val randomName = File.createTempFile(
                System.currentTimeMillis()
                    .toString() + ("" + Math.random()).replace("\\.".toRegex(), ""),
                ".jar"
            )
//    val randomName = File("/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/abc.jar")
            val fos = FileOutputStream(randomName)
            createJarFromClassPathResources(fos, randomName.absolutePath)
            randomName.absolutePath
        } catch (e: IOException) {
            throw e
        }
    }

    @Throws(IOException::class)
    private fun createJarFromClassPathResources(
        fos: FileOutputStream,
        location: String
    ) {
        val m = Manifest()
        m.clear()
        val global = m.mainAttributes
        if (global.getValue(Attributes.Name.MANIFEST_VERSION) == null) {
            global[Attributes.Name.MANIFEST_VERSION] = "1.0"
        }
        val myLocation = File(location)
        global.putValue(
            "Boot-Class-Path",
            getBootClassPath(myLocation)
        )

        global.putValue(
            "Agent-Class",
            MocoAgent::class.java.name
        )

        global.putValue("Can-Redefine-Classes", "true")
        global.putValue("Can-Retransform-Classes", "true")


        global.putValue(
            "Premain-Class",
            MocoAgent::class.java.name
        )

        global.putValue("Can-Set-Native-Method-Prefix", "true")
        JarOutputStream(fos, m).use {}
    }


    @Throws(IOException::class)
    private fun addClass(clazz: Class<*>, jos: JarOutputStream) {
        val className = clazz.name
        val ze = ZipEntry(className.replace(".", "/") + ".class")
        jos.putNextEntry(ze)
        val temp = clsLoader.getByteCodeArray(className)
        if (temp != null) {
            jos.write(temp)
        }
        jos.closeEntry()
    }

    private fun getBootClassPath(myLocation: File): String {
        return myLocation.absolutePath.replace('\\', '/')
    }


    private fun removeAgent(location: String?) {
        if (location != null) {
            val f = File(location)
            f.delete()
        }
    }
}