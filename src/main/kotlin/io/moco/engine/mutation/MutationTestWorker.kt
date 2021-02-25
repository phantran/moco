package io.moco.engine.mutation

import io.moco.engine.io.ByteArrayLoader
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
    private val logger = MoCoLogger()

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MoCoLogger.useKotlinLog()
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
            classPath = givenWorkerArgs.classPath
            val byteArrLoader = ByteArrayLoader(classPath)
            mutantIntroducer = MutantIntroducer(byteArrLoader)
            outputStream = DataOutputStream(socket.getOutputStream())
            TestItemWrapper.configuredTestTimeOut = if (givenWorkerArgs.testTimeOut.toIntOrNull() != null)
                givenWorkerArgs.testTimeOut.toLong() else -1
            mGen = MutationGenerator(
                byteArrLoader,
                givenWorkerArgs.includedOperators.mapNotNull { Operator.nameToOperator(it) })
            val testItems: List<TestItem> = TestItem.testClassesToTestItems(givenWorkerArgs.tests)
            val wrappedTest: Pair<List<TestItemWrapper>, List<TestResultAggregator>> =
                TestItemWrapper.wrapTestItem(testItems)
            runMutationTests(givenWorkerArgs.mutations, wrappedTest.first)
            finished(0)
        } catch (ex: Throwable) {
            ex.printStackTrace(System.out)
            finished(14)
        }
    }

    @Throws(IOException::class)
    private fun runMutationTests(
        mutations: List<Mutation>, tests: List<TestItemWrapper>
    ) {
        var mutatedClassByteArr: ByteArray? = null
        for (mutation: Mutation in mutations) {
            logger.debug("\n")
            logger.debug("------- Handle mutation of class ${mutation.mutationID.location.className?.getJavaName()}--------------")
            if (mutatedClassByteArr == null) {
                val clsJavaName = mutation.mutationID.location.className?.getJavaName()
                mutatedClassByteArr = mGen.bytesArrayLoader.getByteArray(clsJavaName)
            }
            val t0 = System.currentTimeMillis()
            runOneByOne(mutation, tests, mutatedClassByteArr)
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
        register(mutationId)
        val testResult: MutationTestResult = if (tests.isEmpty()) {
            MutationTestResult(0, MutationTestStatus.RUN_ERROR)
        } else {
            introduceMutantThenExec(
                mutation, createdMutant, tests
            )
        }
        report(mutationId, testResult)
    }


    private fun introduceMutantThenExec(
        mutation: Mutation, mutatedClass: Mutant,
        tests: List<TestItemWrapper>
    ): MutationTestResult {
        val mtr: MutationTestResult
        val t0 = System.currentTimeMillis()
        if (mutantIntroducer.introduce(mutation.mutationID.location.className, clsLoader, mutatedClass.byteArray)
        ) {
            logger.debug("Introduce mutant in " + (System.currentTimeMillis() - t0) + " ms")
            logger.debug("Mutation at line ${mutation.lineOfCode}")
            logger.debug("Mutation operator ${mutation.description}")
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
    fun register(mutationID: MutationID?) {
        outputStream.writeByte(ResultsReceiverThread.register.toInt())
        if (mutationID != null) {
            DataStreamUtils.writeObject(outputStream, mutationID)
        }
        outputStream.flush()
    }

    @Synchronized
    @Throws(IOException::class)
    fun report(
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
    fun finished(exitCode: Int) {
        outputStream.writeByte(ResultsReceiverThread.finished.toInt())
        outputStream.writeInt(exitCode)
        outputStream.flush()
    }
}