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

package io.moco.engine.test

import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.StoppedByUserException
import java.lang.Exception


class JUnit34RunListener(
    private val desc: io.moco.engine.test.Description,
    private val tra: TestResultAggregator
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