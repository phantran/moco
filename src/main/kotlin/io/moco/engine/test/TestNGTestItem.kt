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

import kotlinx.coroutines.*
import org.testng.ITestNGListener
import org.testng.TestNG
import org.testng.xml.XmlClass
import org.testng.xml.XmlSuite
import org.testng.xml.XmlTest
import kotlin.system.measureTimeMillis


class TestNGTestItem(cls: Class<*>, executionTime: Long = -1) : TestItem(cls, executionTime) {

    init {
        testng.addListener(listener as ITestNGListener)
        testng.setVerbose(0)
    }

    companion object {
        // needs to be static as jmockit assumes only a single instance per jvm
        private val testng = TestNG(false)
        private val listener: TestNGRunListener = TestNGRunListener()

    }

    override suspend fun execute(tra: TestResultAggregator, timeOut: Long) {
        var job: Job? = null
        try {

            job = GlobalScope.launch {
                withTimeout(timeOut) {
                    val temp = measureTimeMillis {
                        synchronized (testng) {
                            val suite = XmlSuite()
                            suite.name = cls.name
                            suite.setSkipFailedInvocationCounts(true)
                            val test = XmlTest(suite)
                            test.name = cls.name
                            val xclass = XmlClass(cls.name)
                            test.xmlClasses = listOf(xclass)
                            testng.defaultSuiteName = suite.name
                            testng.setXmlSuites(listOf(suite))
                            listener.setTra(tra)
                            listener.setTestCls(cls)
                            try {
                                testng.run()
                            } finally {
                                listener.setTra(null)
                                listener.setTestCls(null)
                            }
                        }
                    }
                    if (executionTime == -1L) {
                        executionTime = temp
                    }
                    logger.debug("Test ${desc.name} finished after $executionTime ms")
                }
            }
            job.join()
        } catch (ex: Exception) {
            when (ex) {
                is TimeoutCancellationException -> {
                    tra.results.add(TestResult(desc, ex, TestResult.TestState.TIMEOUT))
                    logger.debug("Test ${desc.name} execution TIMEOUT - allowed time $timeOut ms")
                }
            }
            throw ex
        } finally {
            job!!.cancel()
        }
    }
}