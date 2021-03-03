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

import io.moco.engine.mutation.ResultsReceiverThread
import java.io.IOException
import java.lang.Exception
import java.net.ServerSocket

class WorkerProcess(
    private val toBeExecutedWorker: Class<*>,
    private val processArgs: Map<String, Any>,
    private val workerArgs: List<String>
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

        commandsToProcess.addAll(workerArgs)

        val processBuilder = ProcessBuilder(commandsToProcess)
        try {
            process = processBuilder.inheritIO().start()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private fun destroyProcess() {
        process?.destroy()
    }

    fun getProcess(): Process? {
        return process
    }

    fun execMutationTestProcess(rt: ResultsReceiverThread) {
        this.start()
        rt.start()
        try {
            rt.waitUntilFinish()
        } finally {
            this.destroyProcess()
        }
    }

}