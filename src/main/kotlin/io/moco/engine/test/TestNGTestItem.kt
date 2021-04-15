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


class TestNGTestItem(cls: Class<*>, testIdentifier: Any, executionTime: Long = -1) :
    TestItem(cls, testIdentifier, executionTime) {

    override val desc: Description = Description(testIdentifier, this.cls.name)

    init {
        if (!testng.testListeners.contains(listener as ITestNGListener)) {
            testng.addListener(listener as ITestNGListener)
        }
        testng.setVerbose(0)
    }

    companion object {
        // needs to be static as jmockit assumes only a single instance per jvm
        private val testng = TestNG(false)
        private val listener: TestNGRunListener = TestNGRunListener()

        fun getTests(res: MutableList<TestItem>, item: Class<*>) {
            // TODO: support better test splitting by methods later
//            item.declaredMethods.map {
//                if (it.annotations.any { it1 ->
//                        it1.annotationClass.java == org.testng.annotations.Test::class.java
//                    }) {
//                    res.add(JUnit34TestItem(item, it.name, false, -1))
//                }
//            }
            res.add(TestNGTestItem(item, item.name, -1))
        }
    }

    override suspend fun execute(tra: TestResultAggregator, timeOut: Long) {
        var job: Job? = null
        try {
            job = GlobalScope.launch {
                withTimeout(timeOut) {
                    logger.debug("Test ${this@TestNGTestItem} started - timeout after $timeOut ms")
                    val temp = measureTimeMillis {
                        synchronized(testng) {
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
                            listener.setDesc(desc)
                            try {
                                testng.run()
                            } finally {
                                listener.setTra(null)
                                listener.setTestCls(null)
                                listener.setDesc(null)
                            }
                        }
                    }
                    if (executionTime == -1L) {
                        executionTime = temp
                    }
                    logger.debug("Test ${this@TestNGTestItem} finished after $executionTime ms")
                }
            }
            job.join()
        } catch (ex: Exception) {
            when (ex) {
                is TimeoutCancellationException -> {
                    tra.results.add(TestResult(desc, ex, TestResult.TestState.TIMEOUT))
                    logger.debug("Test ${this@TestNGTestItem} execution TIMEOUT - allowed time $timeOut ms")
                }
                else -> logger.info(ex.printStackTrace().toString())
            }
            throw ex
        } finally {
            job!!.cancel()
        }
    }

    override fun toString(): String {
//        return ("${desc.testCls}.${desc.name}()")
        return ("${desc.name}")
    }
}