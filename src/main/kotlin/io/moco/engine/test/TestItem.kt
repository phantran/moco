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

import io.moco.engine.ClassName
import io.moco.utils.MoCoLogger
import kotlinx.coroutines.*
import org.junit.internal.builders.AllDefaultPossibilitiesBuilder
import org.junit.internal.runners.ErrorReportingRunner
import org.junit.runner.Runner
import org.junit.runner.notification.RunListener
import java.lang.Exception

import org.junit.runner.notification.RunNotifier
import org.junit.runners.Suite
import kotlin.system.measureTimeMillis


class TestItem(
    val cls: Class<*>,
) {
    val desc: Description = Description(this.cls.name, this.cls.name)
    var executionTime: Long = -1
    private val logger = MoCoLogger()

    suspend fun execute(tra: TestResultAggregator, timeOut: Long = -1) {
        val runner: Runner = createRunner(cls)
        if (runner is ErrorReportingRunner) {
            logger.error("Error while running test of $cls")
        }
        var job: Job? = null
        try {
            val runNotifier = RunNotifier()
            val listener: RunListener = CustomRunListener(desc, tra)
            runNotifier.addFirstListener(listener)
            job = GlobalScope.launch {
                withTimeout(timeOut) {
                    executionTime = measureTimeMillis {
                        runner.run(runNotifier)
                    }
                    logger.debug("Test ${desc.name} finished after $executionTime ms")
                }
            }
            job.join()
        } catch (e: Exception) {
            when (e) {
                is TimeoutCancellationException -> {
                    tra.results.add(TestResult(desc, e, TestResult.TestState.TIMEOUT))
                    logger.warn("Preprocessing: Test ${desc.name} execution TIMEOUT - allowed time $timeOut ms")
                }
            }
            throw e
        } finally {
            job!!.cancel()
        }

    }

    companion object {
        fun createRunner(cls: Class<*>): Runner {
            val builder = AllDefaultPossibilitiesBuilder(true)
            try {
                return builder.runnerForClass(cls)
            } catch (ex: Throwable) {
                throw RuntimeException(ex)
            }
        }

        fun testClassesToTestItems(testClassNames: List<ClassName>): List<TestItem> {
            // convert from test classes to test items so it can be executed
            var testClsNames = testClassNames.mapNotNull { ClassName.clsNameToClass(it) }
            testClsNames = testClsNames.filter { isNotTestSuite(it) }
            return testClsNames.map {
                TestItem(it)
            }
        }

        private fun isNotTestSuite(cls: Class<*>): Boolean {
            // Ignore test suite class since all normal test classes are recorded.
            if (cls.getAnnotation(Suite.SuiteClasses::class.java) == null) {
                return true
            }
            return false
        }
    }

    override fun toString(): String {
        return ("TestItem [cls=$cls description=$desc]")
    }
}