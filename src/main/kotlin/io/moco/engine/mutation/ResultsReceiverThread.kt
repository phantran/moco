package io.moco.engine.mutation

import io.moco.engine.ClassName
import io.moco.engine.operator.Operator
import io.moco.utils.DataStreamUtils
import java.io.*
import java.net.ServerSocket
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.function.Consumer
import java.util.concurrent.Callable


class ResultsReceiverThread(
    private val socket: ServerSocket,
    private val workerArguments: MutationWorkerArguments,
    private val mutationStorage: MutationStorage,
    private val resultMapping: MutableMap<MutationID, MutationTestResult> = mutableMapOf(),
) {

    companion object {
        const val register: Byte = 1
        const val report: Byte = 2
        const val finished: Byte = 4
    }

    var future: FutureTask<Int>? = null

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

    private fun register(stream: DataInputStream) {
        val mutation: MutationID = DataStreamUtils.readObject(stream)
        this.resultMapping[mutation] = MutationTestResult(1, MutationTestStatus.STARTED)
    }

    private fun report(stream: DataInputStream) {
        val mutation: MutationID = DataStreamUtils.readObject(stream)
        val result: MutationTestResult = DataStreamUtils.readObject(stream)
        this.resultMapping[mutation] = result
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

    @Synchronized
    private fun updateMutationStorage() {
        for (mutation: Mutation in workerArguments.mutations) {
            if (resultMapping.containsKey(mutation.mutationID)) {
                val clsName = mutation.mutationID.location.className?.getJavaName()
                if (clsName != null) {
                    val status = when (resultMapping[mutation.mutationID]?.mutationTestStatus) {
                        MutationTestStatus.KILLED -> "killed"
                        MutationTestStatus.SURVIVED -> "survived"
                        else -> "unknown"
                    }
                    if (mutationStorage.entry.containsKey(clsName)) {
                        mutationStorage.entry[clsName]?.add(Pair(mutation, status))
                    } else {
                        mutationStorage.entry[clsName] = mutableListOf(Pair(mutation, status))
                    }
                }

            }
        }
    }

    class MutationWorkerArguments(
        val mutations: List<Mutation>,
        val tests: List<ClassName>,
        val classPath: String,
        val includedOperators: List<String>,
        val filter: String,
    ) : Serializable
}