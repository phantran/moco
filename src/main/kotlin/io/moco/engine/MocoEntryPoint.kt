package io.moco.engine

import io.moco.engine.io.ByteArrayLoader
import io.moco.engine.mutation.Mutation
import io.moco.engine.mutation.MutationGenerator
import io.moco.engine.operator.Operator
import io.moco.engine.preprocessing.PreprocessorWorker
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ServerSocket

import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest


class MocoEntryPoint(
    private val codeRoot: String,
    private val testRoot: String,
    private val excludedClasses: String,
    private val buildRoot: String,
    runtimeClassPath: MutableList<String>,
    private val compileClassPath: MutableList<String>,
    private val jvm: String

) {
    // Preprocessing step: Parse the targets source code and tests to collect information
    // about code blocks and mapping from classes under test to test classes
    // (which test classes are responsible for which classes under test)

    private var classPath: String
    private var byteArrLoader: ByteArrayLoader

    init {
        val cp = runtimeClassPath.joinToString(separator = File.pathSeparatorChar.toString())
        classPath = "$cp:$codeRoot:$testRoot:$buildRoot"
        byteArrLoader = ByteArrayLoader(cp)
    }

    fun execute() {
        //TODO: replace later
        val createdAgent: String = createTemporaryAgentJar() ?: return

        //TODO: replace included operators by params from mojo configuration
        val temp = listOf("AOR", "LCR", "ROR", "UOI")


        // Preprocessing step
        val workerArgs = mutableListOf(codeRoot, testRoot, excludedClasses, buildRoot)
        val workerProcess = WorkerProcess(
            PreprocessorWorker.javaClass,
            getPreprocessWorkerArgs(createdAgent), workerArgs
        )
        workerProcess.start()
        // Mutation step
        // Mutations collecting
        val toBeMutatedCodeBase = Codebase(codeRoot, testRoot, excludedClasses)
        val includedMutationOperators: List<Operator> = temp.mapNotNull { Operator.nameToOperator(it) }
        val mGen = MutationGenerator(byteArrLoader, includedMutationOperators)
        val foundMutations: Map<ClassName, List<Mutation>> =
            toBeMutatedCodeBase.sourceClassNames.associateWith { mGen.findPossibleMutationsOfClass(it) }


        // Mutation test generating and executing
//        val relatedTests: List<TestItemWrapper> = MutationFinder.retriveRelatedTest()


        // Remove generated agent after finishing

        removeTemporaryAgentJar(createdAgent)
    }

    private fun getPreprocessWorkerArgs(createdAgent: String): MutableMap<String, Any> {
        // TODO: will be replaced by configuration from Maven configuration parameters
        val preprocessWorkerArgs: MutableMap<String, Any> = mutableMapOf()
        preprocessWorkerArgs["port"] = ServerSocket(0)
        val javaBin = jvm
        preprocessWorkerArgs["javaExecutable"] = javaBin
        preprocessWorkerArgs["javaAgentJarPath"] = "-javaagent:$createdAgent"
        preprocessWorkerArgs["classPath"] = classPath
        return preprocessWorkerArgs
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