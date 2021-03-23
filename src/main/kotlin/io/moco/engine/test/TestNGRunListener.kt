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

import org.testng.ITestContext
import org.testng.ITestListener
import org.testng.ITestResult


class TestNGRunListener : ITestListener {
    private var tra: TestResultAggregator? = null
    private var testClass: Class<*>? = null

    fun setTra(tra: TestResultAggregator?) {
        this.tra = tra
    }

    fun setTestCls(cls: Class<*>?) {
        this.testClass = cls
    }

    fun description(result: ITestResult): Description {
        return Description(result.method.methodName, testClass?.name)
    }

    override fun onTestStart(result: ITestResult) {
        tra?.results?.add(TestResult(description(result), null, TestResult.TestState.RUNNING))
    }

    override fun onTestSuccess(result: ITestResult) {
        tra?.results?.add(TestResult(description(result), null, TestResult.TestState.FINISHED))
    }

    override fun onTestFailure(result: ITestResult) {
        tra?.results?.add(TestResult(description(result), result.throwable, TestResult.TestState.FINISHED))
    }

    override fun onTestSkipped(result: ITestResult) {
        tra?.results?.add(TestResult(description(result), null, TestResult.TestState.NOT_STARTED))
    }

    override fun onTestFailedButWithinSuccessPercentage(result: ITestResult) {
        tra?.results?.add(TestResult(description(result), null, TestResult.TestState.FINISHED))
    }

    override fun onStart(p0: ITestContext?) {
        tra?.results?.add(TestResult(Description("", testClass?.name),
                                            null, TestResult.TestState.RUNNING))
    }

    override fun onFinish(p0: ITestContext?) {
        tra?.results?.add(TestResult(Description("", testClass?.name),
            null, TestResult.TestState.FINISHED))
    }
}
