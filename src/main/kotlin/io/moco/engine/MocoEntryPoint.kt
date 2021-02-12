package io.moco.engine

import io.moco.engine.io.BytecodeLoader
import io.moco.engine.mutation.Mutant
import io.moco.engine.mutation.MutationFinder
import io.moco.engine.mutation.MutationGenerator
import io.moco.engine.operator.Operator
import io.moco.engine.preprocessing.PreprocessorWorker
import java.io.File
import java.net.ServerSocket

class MocoEntryPoint(
    private val codeRoot: String,
    private val testRoot: String,
    private val excludedClasses: String,
    private val buildRoot: String
) {
    // Preprocessing step: Parse the targets source code and tests to collect information
    // about code blocks and mapping from classes under test to test classes
    // (which test classes are responsible for which classes under test)

    fun execute() {
        //TODO: replace later
        val classpath = System.getProperty("java.class.path")
        val temp0 = "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/test-classes"
        val temp1 =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin"
        val temp2 =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/classes"
        val cp = "$classpath:$temp0:$temp1:$temp2"
        //TODO: replace included operators by params from mojo configuration
        val temp = listOf<String>("AOR", "LCR", "ROR", "UOI")


        // Preprocessing step
        val workerArgs = mutableListOf(codeRoot, testRoot, excludedClasses, buildRoot)
        val workerProcess = WorkerProcess(PreprocessorWorker.javaClass,
            getPreprocessWorkerArgs(cp), workerArgs)
        workerProcess.start()

        // Mutation step
        // Mutations collecting
        val toBeMutatedCodeBase = Codebase(codeRoot, testRoot, excludedClasses)
        val includedMutationOperators: List<Operator> = temp.mapNotNull { Operator.nameToOperator(it) }
        val clsLoader = BytecodeLoader(cp)
        val mutationFinder = MutationFinder(clsLoader, includedMutationOperators)
        val mGen = MutationGenerator(toBeMutatedCodeBase, mutationFinder)
        val foundMutations: Map<ClassName, List<Mutant>> = mGen.codeBaseMutationAnalyze()

        // Mutation test generating and executing


    }

    private fun getPreprocessWorkerArgs(cp: Any): MutableMap<String, Any> {
        // TODO: will be replaced by configuration from Maven configuration parameters
        val preprocessWorkerArgs: MutableMap<String, Any> = mutableMapOf()
        preprocessWorkerArgs["port"] = ServerSocket(0)
        val javaHome = System.getProperty("java.home")
        val javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java"
        preprocessWorkerArgs["javaExecutable"] = javaBin
        val agentArg = "-javaagent:MyJar.jar"
        preprocessWorkerArgs["javaAgentJarPath"] = agentArg
        preprocessWorkerArgs["classPath"] = cp
        return preprocessWorkerArgs
    }
}