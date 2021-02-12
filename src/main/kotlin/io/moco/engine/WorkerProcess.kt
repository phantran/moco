package io.moco.engine

import java.io.IOException
import java.lang.Exception
import java.net.ServerSocket

class WorkerProcess(
    private val toBeExecutedWorker: Class<*>,
    private val processArgs: Map<String, Any>,
    private val workerArgs: List<String>?
) {
    private var process: Process? = null

    @Throws(IOException::class)
    fun start() {
        val commandsToProcess: MutableList<String> = mutableListOf()
        commandsToProcess.add(processArgs["javaExecutable"] as String)
        if (processArgs["javaAgentJarPath"] != null) {
            commandsToProcess.add(processArgs["javaAgentJarPath"] as String)
        }
        if (processArgs["classPath"] != null) {
            commandsToProcess.add("-cp")
            commandsToProcess.add(processArgs["classPath"] as String)
        }
        commandsToProcess.add(toBeExecutedWorker.name)
        val port = processArgs["port"] as ServerSocket
        commandsToProcess.add(port.localPort.toString())
        if (workerArgs != null) {
            commandsToProcess.addAll(workerArgs)
        }
        val processBuilder = ProcessBuilder(commandsToProcess)
        try {
            process = processBuilder.inheritIO().start()
            process?.waitFor()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    fun getProcess(): Process? {
        return process
    }
}