package io.moco.engine.preprocessing

import java.io.IOException

class PreprocessorWorker(private val toBeExecutedCls: Class<*>,
                         private val processArgs: Map<String, String>) {
    private var process: Process? = null

    @Throws(IOException::class)
    fun start() {
        val commandsToProcess: MutableList<String> = mutableListOf()
        commandsToProcess.add(processArgs["javaExecutable"] as String)
        if (processArgs["javaAgentPath"] != null) {
            commandsToProcess.add("-javaagent:" + processArgs["javaAgentPath"] as String)
        }
        if (processArgs["classPath"] != null) {
            commandsToProcess.add("-cp " + processArgs["classPath"] as String)
        }
        commandsToProcess.add(toBeExecutedCls.name)
        commandsToProcess.add(processArgs["workerArgs"] as String)

        val processBuilder = ProcessBuilder(commandsToProcess)
        process = processBuilder.inheritIO().start()
    }

    fun getProcess(): Process? {
        return process
    }
}