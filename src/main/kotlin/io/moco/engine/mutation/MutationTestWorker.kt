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

import io.moco.utils.ByteArrayLoader
import io.moco.engine.operator.Operator
import io.moco.engine.test.TestItem
import io.moco.engine.test.TestItemWrapper
import io.moco.engine.test.TestResult
import io.moco.engine.test.TestResultAggregator
import io.moco.utils.ClassLoaderUtil
import io.moco.utils.DataStreamUtils
import io.moco.utils.MoCoLogger
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket


class MutationTestWorker(
    private val socket: Socket,
) {
    private lateinit var outputStream: DataOutputStream
    private lateinit var classPath: String
    private lateinit var mGen: MutationGenerator
    private lateinit var mutantIntroducer: MutantIntroducer
    private val clsLoader = ClassLoaderUtil.contextClsLoader
    private lateinit var logger: MoCoLogger
    private var duplicatedMutantTracker: MutableSet<ByteArray> = mutableSetOf()

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

            MoCoLogger.useKotlinLog()
            logger = MoCoLogger()
            classPath = givenWorkerArgs.classPath

            val byteArrLoader = ByteArrayLoader(classPath)
            mutantIntroducer = MutantIntroducer(byteArrLoader)
            outputStream = DataOutputStream(socket.getOutputStream())

            TestItemWrapper.configuredTestTimeOut = if (givenWorkerArgs.testTimeOut.toIntOrNull() != null)
                givenWorkerArgs.testTimeOut.toLong() else -1
            mGen = MutationGenerator(
                byteArrLoader, givenWorkerArgs.includedOperators.mapNotNull { Operator.nameToOperator(it) })
            val testItems: List<TestItem> = TestItem.testClassesToTestItems(givenWorkerArgs.tests)
            val wrappedTest: Pair<List<TestItemWrapper>, List<TestResultAggregator>> =
                TestItemWrapper.wrapTestItem(testItems)
            runMutationTests(givenWorkerArgs.mutations, wrappedTest.first)
            finishedMessageToMainProcess(0)
        } catch (ex: Throwable) {
            ex.printStackTrace(System.out)
            finishedMessageToMainProcess(14)
        }
    }

    @Throws(IOException::class)
    private fun runMutationTests(
        mutations: List<Mutation>, tests: List<TestItemWrapper>
    ) {
        var targetClassByteArr: ByteArray? = null // original class byte array
        for (mutation: Mutation in mutations) {
            logger.debug("------- Handle mutation of class ${mutation.mutationID.location.className?.getJavaName()}--------------")
            if (targetClassByteArr == null) {
                val clsJavaName = mutation.mutationID.location.className?.getJavaName()
                targetClassByteArr = mGen.bytesArrayLoader.getByteArray(clsJavaName)
            }
            val t0 = System.currentTimeMillis()
            runOneByOne(mutation, tests, targetClassByteArr)
            logger.debug("------- Done in " + (System.currentTimeMillis() - t0) + " ms -------------")
        }
    }

    @Throws(IOException::class)
    private fun runOneByOne(
        mutation: Mutation,
        tests: List<TestItemWrapper>, byteArray: ByteArray?
    ) {
        val mutationId = mutation.mutationID
        val createdMutant = mGen.createMutant(mutationId, byteArray) ?: return

        // Filter out duplicated mutants which have same byte array (equivalent mutant)
        if (duplicatedMutantTracker.contains(createdMutant.byteArray)) {
            return
        } else {
            duplicatedMutantTracker.add(createdMutant.byteArray)
        }

        registerToMainProcess(mutationId)
        val testResult: MutationTestResult = if (tests.isEmpty()) {
            MutationTestResult(0, MutationTestStatus.RUN_ERROR)
        } else {
            introduceMutantThenExec(
                mutation, createdMutant, tests
            )
        }
        reportToMainProcess(mutationId, testResult)
    }


    private fun introduceMutantThenExec(
        mutation: Mutation, mutatedClass: Mutant,
        tests: List<TestItemWrapper>
    ): MutationTestResult {
        val mtr: MutationTestResult
        val t0 = System.currentTimeMillis()
        if (mutantIntroducer.introduce(mutation.mutationID.location.className, clsLoader, mutatedClass.byteArray)) {
            logger.debug("Introduce mutant in " + (System.currentTimeMillis() - t0) + " ms")
            logger.debug("Mutation at line ${mutation.lineOfCode}")
            logger.debug("Mutation operator: ${mutation.description}")
            mtr = executeTestAndGetResult(tests)
        } else {
            return MutationTestResult(0, MutationTestStatus.RUN_ERROR)

        }
        return mtr
    }

    private fun executeTestAndGetResult(
        tests: List<TestItemWrapper>
    ): MutationTestResult {
        var killed = false
        var numberOfExecutedTests = 0
        var finalStatus: MutationTestStatus
        try {
            runBlocking {
                for (test: TestItemWrapper? in tests) {
                    try {
                        numberOfExecutedTests += 1
                        test?.call()
                        // A mutant is killed if a test is failed
                        killed = checkIfMutantWasKilled(test?.testResultAggregator)
                        if (killed) {
                            break
                        }
                    } catch (e: Exception) {
                        logger.error("Error while executing test ${test?.testItem?.desc?.name}")
                    }
                }

                finalStatus = if (killed) MutationTestStatus.KILLED else MutationTestStatus.SURVIVED
                if (finalStatus == MutationTestStatus.SURVIVED) {
                    if (numberOfExecutedTests == tests.size &&
                        tests.all { it.testResultAggregator.results.any { it1 -> it1.error != null } } ) {
                        // A mutant is not erroneous if all tests ran against it have thrown errors
                        finalStatus = MutationTestStatus.RUN_ERROR
                    }
                }
                // Reset test result aggregator of test classes before moving to the next mutant
                tests.map { it.testResultAggregator.results.clear() }
            }
            return MutationTestResult(numberOfExecutedTests, finalStatus)
        } catch (ex: Exception) {
            return MutationTestResult(numberOfExecutedTests, MutationTestStatus.RUN_ERROR)
        }

    }

    private fun checkIfMutantWasKilled(tra: TestResultAggregator?): Boolean {
        if (tra != null) {
            for (r: TestResult in tra.results) {
                if ((r.state == TestResult.TestState.FINISHED) && (r.error != null)) {
                    return true
                }
            }
        }
        return false
    }

    @Synchronized
    @Throws(IOException::class)
    fun registerToMainProcess(mutationID: MutationID?) {
        outputStream.writeByte(ResultsReceiverThread.register.toInt())
        if (mutationID != null) {
            DataStreamUtils.writeObject(outputStream, mutationID)
        }
        outputStream.flush()
    }

    @Synchronized
    @Throws(IOException::class)
    fun reportToMainProcess(
        mutationID: MutationID?,
        mutationTestResult: MutationTestResult?
    ) {
        outputStream.writeByte(ResultsReceiverThread.report.toInt())
        if (mutationID != null) {
            DataStreamUtils.writeObject(outputStream, mutationID)
        }
        if (mutationTestResult != null) {
            DataStreamUtils.writeObject(outputStream, mutationTestResult)
        }
        outputStream.flush()
    }

    @Synchronized
    @Throws(IOException::class)
    fun finishedMessageToMainProcess(exitCode: Int) {
        outputStream.writeByte(ResultsReceiverThread.finished.toInt())
        outputStream.writeInt(exitCode)
        outputStream.flush()
    }
}