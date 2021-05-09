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

package io.github.phantran.engine.test

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier

class JUnit5RunListener(
    private val desc: Description,
    private val tra: TestResultAggregator
) : TestExecutionListener {

    override fun executionSkipped(testIdentifier: TestIdentifier, reason: String?) {
        if (testIdentifier.isTest) {
            tra.results.add(TestResult(desc, null, TestResult.TestState.NOT_STARTED))
        }
    }

    override fun executionStarted(testIdentifier: TestIdentifier) {
        if (testIdentifier.isTest) {
            tra.results.add(TestResult(desc, null, TestResult.TestState.RUNNING))
        }
    }

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (testIdentifier.isTest) {
            val exception = testExecutionResult.throwable
            if (TestExecutionResult.Status.ABORTED == testExecutionResult.status) {
                tra.results.add(TestResult(desc, null, TestResult.TestState.FINISHED))
            } else if (exception.isPresent) {
                tra.results.add(TestResult(desc, exception.get(), TestResult.TestState.FINISHED))
            } else {
                tra.results.add(TestResult(desc, null, TestResult.TestState.FINISHED))
            }
        }
    }
}