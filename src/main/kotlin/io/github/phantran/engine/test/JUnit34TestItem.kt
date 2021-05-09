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

import io.github.phantran.utils.MoCoLogger
import junit.framework.TestCase
import kotlinx.coroutines.*
import org.junit.internal.builders.AllDefaultPossibilitiesBuilder
import org.junit.internal.runners.ErrorReportingRunner
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.Runner
import org.junit.runner.notification.RunListener
import java.lang.Exception

import org.junit.runner.notification.RunNotifier
import kotlin.system.measureTimeMillis
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.BlockJUnit4ClassRunner


class JUnit34TestItem(
    cls: Class<*>, testIdentifier: Any,
    private val isJUnit3: Boolean, executionTime: Long = -1
) : TestItem(cls, testIdentifier, executionTime) {

    override val desc: Description = Description(testIdentifier, this.cls.name)

    override suspend fun execute(tra: TestResultAggregator, timeOut: Long) {
//        val runner: Runner = createRunner(cls, testIdentifier as String, isJUnit3)
        val runner: Runner? = createRunner(cls)
        if (runner is ErrorReportingRunner || runner == null) {
            logger.debug("Error while creating test runner for $cls")
            return
        }
        var job: Job? = null
        try {
            val runNotifier = RunNotifier()
            val listener: RunListener = JUnit34RunListener(desc, tra)
            runNotifier.addFirstListener(listener)
            job = GlobalScope.launch {
                logger.debug("Test ${this@JUnit34TestItem} started - timeout after $timeOut ms")
                withTimeout(timeOut) {
                    val temp = measureTimeMillis {
                        runner.run(runNotifier)
                    }
                    if (executionTime == -1L) {
                        executionTime = temp
                    }
                    logger.debug("Test ${this@JUnit34TestItem}  finished after $executionTime ms")
                }
            }
            job.join()
        } catch (e: Exception) {
            when (e) {
                is TimeoutCancellationException -> {
                    tra.results.add(TestResult(desc, e, TestResult.TestState.TIMEOUT))
                    logger.debug("Test ${this@JUnit34TestItem}  execution TIMEOUT - allowed time $timeOut ms")
                }
                else -> logger.info(e.printStackTrace().toString())
            }
            throw e
        } finally {
            job!!.cancel()
        }
    }

    @Suppress("DEPRECATION")
    companion object {
        val logger = MoCoLogger()
        // TODO: support tests by methods later
//        fun createRunner(cls: Class<*>, methodName: String, isJUnit3: Boolean): Runner {
//            if (isJUnit3) {
//                val test = cls.newInstance() as TestCase
//                test.name = methodName
//                return JUnit38ClassRunner(test)
//            } else {
//                return object : BlockJUnit4ClassRunner(cls) {
//                    override fun computeTestMethods(): List<FrameworkMethod>? {
//                        return try {
//                            val method = cls.getMethod(methodName)
//                            listOf(FrameworkMethod(method))
//                        } catch (e: Exception) {
//                            logger.warn("Cannot create runner for this test")
//                            null
//                        }
//                    }
//                }
//            }
//        }

        fun createRunner(cls: Class<*>): Runner? {
            val builder = AllDefaultPossibilitiesBuilder(true)
            return try {
                builder.runnerForClass(cls)
            } catch (ex: Throwable) {
                logger.info(ex.printStackTrace().toString())
                null
            }
        }


        fun getTests(res: MutableList<TestItem>, isJUnit3: Boolean, item: Class<*>) {
            if (isJUnit3) {
                // Junit 3
                // TODO: support tests by methods later
//                item.declaredMethods.map {
//                    if (it.name.startsWith("test")) {
//                        res.add(JUnit34TestItem(item, it.name, true, -1))
//                    }
//                }
                res.add(JUnit34TestItem(item, item.name, true, -1))
            } else {
                // Junit 4
                // TODO: support tests by methods later
//                item.declaredMethods.map {
//                    if (it.annotations.any { it1 ->
//                            it1.annotationClass.java == org.junit.Test::class.java
//                        }) {
//                        res.add(JUnit34TestItem(item, it.name, false, -1))
//                    }
//                }
                res.add(JUnit34TestItem(item, item.name, false, -1))

            }
        }
    }

    override fun toString(): String {
//        return ("${desc.testCls}.${desc.name}()")
        return ("${desc.name}")
    }
}