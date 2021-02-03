package io.moco.engine.test

import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.StoppedByUserException
import java.lang.Exception


internal class CustomRunListener(
    val desc: io.moco.engine.test.Description,
    val tra: TestResultAggregator
) : RunListener() {

    private var failed = false

    override fun testAssumptionFailure(failure: Failure) {
    }

    @Throws(Exception::class)
    override fun testIgnored(description: Description?) {
        tra.results.add(TestResult(desc, null, TestResult.TestState.NOT_STARTED))
    }

    @Throws(Exception::class)
    override fun testFailure(failure: Failure) {
        failed = true
        tra.results.add(TestResult(desc, failure.exception, TestResult.TestState.FINISHED))
    }

    @Throws(Exception::class)
    override fun testStarted(description: Description) {
        if (failed) {
            throw StoppedByUserException()
        }
        tra.results.add(TestResult(desc, null, TestResult.TestState.RUNNING))
    }

    @Throws(Exception::class)
    override fun testFinished(description: Description) {
        if (!failed) {
            tra.results.add(TestResult(desc, null, TestResult.TestState.FINISHED))
        }
    }
}