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

package io.moco.engine.mutation

import io.moco.engine.MoCoProcessCode
import io.moco.utils.ByteArrayLoader
import io.moco.engine.operator.Operator
import io.moco.engine.test.TestItem
import io.moco.engine.test.TestItemWrapper
import io.moco.engine.test.TestResultAggregator
import io.moco.utils.DataStreamUtils
import io.moco.utils.MoCoLogger
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket


class MutationTestWorker(
    private val socket: Socket,
) {
    private lateinit var classPath: String
    private lateinit var mGen: MutationGenerator
    private lateinit var mutantIntroducer: MutantIntroducer
    private lateinit var communicator: Communicator
    private lateinit var logger: MoCoLogger
    private var duplicatedMutantTracker: MutableSet<ByteArray> = mutableSetOf()
    private val testMonitor = MutationTestMonitor()
    private lateinit var testsExecutionTime: MutableMap<String, Long>

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val port = Integer.valueOf(args[0])
            var socket: Socket? = null
            try {
                socket = Socket("localhost", port)
                val worker = MutationTestWorker(socket)
                worker.run()
            } catch (e: Throwable) {
                e.printStackTrace(System.out)
            } finally {
                socket?.close()
            }
        }
    }

    fun run() {
        try {
            val givenWorkerArgs: ResultsReceiverThread.MutationWorkerArguments =
                DataStreamUtils.readObject(DataInputStream(socket.getInputStream()))
            MoCoLogger.debugEnabled = givenWorkerArgs.debugEnabled
            MoCoLogger.verbose = givenWorkerArgs.verbose
            if (givenWorkerArgs.noLogAtAll) MoCoLogger.noLogAtAll = true
            MoCoLogger.useKotlinLog()
            logger = MoCoLogger()
            classPath = givenWorkerArgs.classPath
            testsExecutionTime = givenWorkerArgs.testsExecutionTime
            val byteArrLoader = ByteArrayLoader(classPath)
            mutantIntroducer = MutantIntroducer(byteArrLoader)
            communicator = Communicator(DataOutputStream(socket.getOutputStream()))
            TestItemWrapper.configuredTestTimeOut = if (givenWorkerArgs.testTimeOut.toIntOrNull() != null)
                givenWorkerArgs.testTimeOut.toLong() else -1
            mGen = MutationGenerator(
                byteArrLoader, givenWorkerArgs.includedOperators.mapNotNull { Operator.nameToOperator(it) })
            val testItems: List<TestItem> = TestItem.testClassesToTestItems(givenWorkerArgs.tests, testsExecutionTime)
            val wrappedTest: Pair<List<TestItemWrapper>, List<TestResultAggregator>> =
                TestItemWrapper.wrapTestItem(testItems)
            runMutationTests(givenWorkerArgs.mutations, wrappedTest.first)
            communicator.finishedMessageToMainProcess(MoCoProcessCode.OK.code)
        } catch (ex: Throwable) {
            ex.printStackTrace(System.out)
            communicator.finishedMessageToMainProcess(MoCoProcessCode.ERROR.code)
        }
    }

    @Throws(IOException::class)
    private fun runMutationTests(
        mutations: List<Mutation>, tests: List<TestItemWrapper>
    ) {
        var targetClassByteArr: ByteArray? = null // original class byte array
        MutationTestExecutor.testMonitor = testMonitor
        for (mutation: Mutation in mutations) {
            if (testMonitor.shouldSkipThisMutation(mutation)) {
                logger.debug("Skip mutation test for mutant at line " +
                        "${mutation.lineOfCode} - ${mutation.mutationID.mutatorID}")
                continue
            }
            logger.debug("------- Handle mutation of class " +
                    "${mutation.mutationID.location.className?.getJavaName()} --------------")
            if (targetClassByteArr == null) {
                val clsJavaName = mutation.mutationID.location.className?.getJavaName()
                targetClassByteArr = mGen.bytesArrayLoader.getByteArray(clsJavaName)
            }
            runOneByOne(mutation, tests, targetClassByteArr)
        }
    }

    @Throws(IOException::class)
    private fun runOneByOne(
        mutation: Mutation, tests: List<TestItemWrapper>, byteArray: ByteArray?
    ) {
        val t0 = System.currentTimeMillis()
        val createdMutant = mGen.createMutant(mutation, byteArray) ?: return
        val mutationId = mutation.mutationID
        // Filter out duplicated mutants which have same byte array (equivalent mutant)
        if (duplicatedMutantTracker.contains(createdMutant.byteArray)) return
        else duplicatedMutantTracker.add(createdMutant.byteArray)


        communicator.registerToMainProcess(mutationId)

        val testResult: MutationTestResult = if (tests.isEmpty()) {
            MutationTestResult(0, MutationTestStatus.RUN_ERROR)
        } else {
            MutationTestExecutor.introduceMutantThenExec(
                mutantIntroducer, mutation, createdMutant, tests
            )
        }

        communicator.reportToMainProcess(mutation, testResult)
        logger.debug("------- Done in " + (System.currentTimeMillis() - t0) + " ms -------------")
    }
}