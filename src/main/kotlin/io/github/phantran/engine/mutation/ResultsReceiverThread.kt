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

package io.github.phantran.engine.mutation

import io.github.phantran.engine.Configuration
import io.github.phantran.engine.test.SerializableTestInfo
import io.github.phantran.persistence.MutationStorage
import io.github.phantran.utils.DataStreamUtils
import java.io.*
import java.net.ServerSocket
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.function.Consumer
import java.util.concurrent.Callable


class ResultsReceiverThread(
    private val socket: ServerSocket,
    private val workerArguments: MutationWorkerArguments,
    @get: Synchronized
    private val mutationStorage: MutationStorage,
    @get: Synchronized
    private val resultMapping: MutableMap<MutationID, MutableMap<String, Any>> = mutableMapOf(),
) {

    companion object {
        const val register: Byte = 1
        const val report: Byte = 2
        const val finished: Byte = 4
    }

    var future: FutureTask<Int>? = null

    @get: Synchronized
    private val sendArgumentsToWorker = Consumer { outputStream: DataOutputStream ->
        DataStreamUtils.writeObject(outputStream, workerArguments)
    }

    @Throws(IOException::class, InterruptedException::class)
    fun start() {
        future = FutureTask(resultPolling)
        val thread = Thread(future)
        thread.isDaemon = true
        thread.name = "Moco results receiver thread"
        thread.start()
    }

    fun waitUntilFinish(): Int {
        return try {
            future!!.get()
        } catch (e: ExecutionException) {
            throw e
        } catch (e: InterruptedException) {
            throw e
        }
    }

    @Synchronized
    private fun register(stream: DataInputStream) {
        val mutationID: MutationID = DataStreamUtils.readObject(stream)
        this.resultMapping[mutationID] =
            mutableMapOf("result" to MutationTestResult(1, MutationTestStatus.STARTED))
    }

    @Synchronized
    private fun report(stream: DataInputStream) {
        val mutationID: MutationID = DataStreamUtils.readObject(stream)
        val instructionsOder: List<String> = DataStreamUtils.readObject(stream)
        val additionalInfo: MutableMap<String, String> = DataStreamUtils.readObject(stream)
        val result: MutationTestResult = DataStreamUtils.readObject(stream)
        this.resultMapping[mutationID]?.set("result", result)
        this.resultMapping[mutationID]?.put("instructionsOder", instructionsOder)
        this.resultMapping[mutationID]?.put("additionalInfo", additionalInfo)
    }

    private val resultPolling = Callable {
        try {
            socket.accept().use { s ->
                try {
                    BufferedInputStream(
                        s.getInputStream()
                    ).use { bufferedStream ->
                        // Send arguments to mutation worker process
                        sendArgumentsToWorker.accept(DataOutputStream(s.getOutputStream()))

                        val inputStream = DataInputStream(bufferedStream)
                        var signal: Byte = inputStream.readByte()
                        // Polling -> read data sent from mutation process worker thread
                        while (signal != finished) {
                            when (signal) {
                                register -> register(inputStream)
                                report -> report(inputStream)
                            }
                            signal = inputStream.readByte()
                        }
                        if (signal == finished) {
                            updateMutationStorage()
                        }
                        return@Callable inputStream.readInt()
                    }
                } catch (e: IOException) {
                    throw e
                }
            }
        } finally {
            try {
                socket.close()
            } catch (e: IOException) {
                throw e
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    private fun updateMutationStorage() {
        for (mutation: Mutation in workerArguments.mutations) {
            if (resultMapping.containsKey(mutation.mutationID)) {
                val clsName = mutation.mutationID.location.className?.getJavaName()
                if (clsName != null) {
                    val testRes = resultMapping[mutation.mutationID]?.get("result") as MutationTestResult
                    val status = when (testRes.mutationTestStatus) {
                        MutationTestStatus.KILLED -> "killed"
                        MutationTestStatus.SURVIVED -> "survived"
                        else -> "run_error"
                    }
                    val killedByTest = when (testRes.killedByTest) {
                        null, "null" -> "None"
                        else -> testRes.killedByTest
                    }
                    val collectInstructionsOrder =
                        resultMapping[mutation.mutationID]?.get("instructionsOder") as List<String>
                    val additionalInfo =
                        resultMapping[mutation.mutationID]?.get("additionalInfo") as MutableMap<String, String>
                    mutation.instructionsOrder = collectInstructionsOrder.toMutableList()
                    mutation.additionalInfo = additionalInfo

                    // storage already contains class name entry
                    if (mutationStorage.entries.containsKey(clsName)) {
                        // NOTE: do not change map keys because of the consistency between moco and gamekins
                        val existing = mutationStorage.entries[clsName]?.find { it["mutationDetails"] == mutation }
                        if (existing != null && status == "killed") {
                            existing["result"] = status
                            existing["killedByTest"] = killedByTest
                            existing["uniqueID"] = mutation.hashCode()
                        } else {
                            mutationStorage.entries[clsName]?.add(
                                mutableMapOf(
                                    "mutationDetails" to mutation,
                                    "result" to status,
                                    "killedByTest" to killedByTest,
                                    "uniqueID" to mutation.hashCode()
                                )
                            )
                        }
                    } else {
                        // add new class name entry to storage
                        mutationStorage.entries[clsName] = mutableSetOf(
                            mutableMapOf(
                                "mutationDetails" to mutation,
                                "result" to status,
                                "killedByTest" to killedByTest,
                                "uniqueID" to mutation.hashCode()
                            )
                        )
                    }
                }
            }
        }
    }

    class MutationWorkerArguments(
        val mutations: List<Mutation>,
        val lineTestsMapping: MutableMap<Int, MutableSet<SerializableTestInfo>>,
        val classPath: String,
        val includedOperators: List<String>,
        val filter: String,
        val testTimeOut: String = Configuration.currentConfig!!.preprocessTestTimeout,
        val debugEnabled: Boolean = Configuration.currentConfig?.debugEnabled == true,
        val verbose: Boolean = Configuration.currentConfig?.verbose == true,
        val noLogAtAll: Boolean = Configuration.currentConfig?.noLogAtAll == true
    ) : Serializable
}