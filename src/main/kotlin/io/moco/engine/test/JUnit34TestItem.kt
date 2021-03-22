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
import org.junit.internal.builders.AllDefaultPossibilitiesBuilder
import org.junit.internal.runners.ErrorReportingRunner
import org.junit.runner.Runner
import org.junit.runner.notification.RunListener
import java.lang.Exception

import org.junit.runner.notification.RunNotifier
import kotlin.system.measureTimeMillis


class JUnit34TestItem(cls: Class<*>, executionTime: Long = -1): TestItem(cls, executionTime) {

    override suspend fun execute(tra: TestResultAggregator, timeOut: Long) {
        val runner: Runner = createRunner(cls)
        if (runner is ErrorReportingRunner) {
            logger.debug("Error while running test of $cls")
        }
        var job: Job? = null
        try {
            val runNotifier = RunNotifier()
            val listener: RunListener = JUnit34RunListener(desc, tra)
            runNotifier.addFirstListener(listener)
            job = GlobalScope.launch {
                withTimeout(timeOut) {
                    val temp = measureTimeMillis {
                        runner.run(runNotifier)
                    }
                    if (executionTime == -1L) {
                        executionTime = temp
                    }
                    logger.debug("Test ${desc.name} finished after $executionTime ms")
                }
            }
            job.join()
        } catch (e: Exception) {
            when (e) {
                is TimeoutCancellationException -> {
                    tra.results.add(TestResult(desc, e, TestResult.TestState.TIMEOUT))
                    logger.debug("Test ${desc.name} execution TIMEOUT - allowed time $timeOut ms")
                }
            }
            throw e
        } finally {
            job!!.cancel()
        }

    }

    @Suppress("DEPRECATION")
    companion object {
        fun createRunner(cls: Class<*>): Runner {
            val builder = AllDefaultPossibilitiesBuilder(true)
            try {
                return builder.runnerForClass(cls)
            } catch (ex: Throwable) {
                throw RuntimeException(ex)
            }
        }
    }

    override fun toString(): String {
        return ("TestItem [cls=$cls description=$desc executionTime=$executionTime]")
    }
}