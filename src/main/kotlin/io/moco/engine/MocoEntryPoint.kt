package io.moco.engine

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
        // Preprocessing step
        val workerArgs = mutableListOf(codeRoot, testRoot, excludedClasses, buildRoot)
        val workerProcess = WorkerProcess(PreprocessorWorker.javaClass,
            getPreprocessWorkerArgs(), workerArgs)
        workerProcess.start()

        // Mutation step

    }

    private fun getPreprocessWorkerArgs(): MutableMap<String, Any> {
        val preprocessWorkerArgs: MutableMap<String, Any> = mutableMapOf()
        preprocessWorkerArgs["port"] = ServerSocket(0)
        val javaHome = System.getProperty("java.home")
        val javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java"
        preprocessWorkerArgs["javaExecutable"] = javaBin
        val agentArg = "-javaagent:MyJar.jar"
        preprocessWorkerArgs["javaAgentJarPath"] = agentArg
        val classpath = System.getProperty("java.class.path")
        val temp = "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/test-classes"
        val temp1 =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin"
        val temp2 =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/classes"
        preprocessWorkerArgs["classPath"] = "$classpath:$temp:$temp1:$temp2"
        return preprocessWorkerArgs
    }
}