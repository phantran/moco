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
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.Launcher
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import java.util.*
import java.util.Collections.unmodifiableList
import kotlin.system.measureTimeMillis


class Junit5TestItem(
    cls: Class<*>, private val testIdentifier: TestIdentifier, executionTime: Long = -1
) : TestItem(cls, executionTime) {

    override val desc = Description(testIdentifier.displayName, cls.name)

    override suspend fun execute(tra: TestResultAggregator, timeOut: Long) {
        var job: Job? = null
        try {
            val launcher = LauncherFactory.create()
            val launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(DiscoverySelectors.selectUniqueId(testIdentifier.uniqueId))
                .build()
            launcher.registerTestExecutionListeners(JUnit5RunListener(desc, tra))

            job = GlobalScope.launch {
                withTimeout(timeOut) {
                    val temp = measureTimeMillis {
                        launcher.execute(launcherDiscoveryRequest)
                    }
                    if (executionTime == -1L) {
                        executionTime = temp
                    }
                    logger.debug("Test ${desc.testCls}.${desc.name} finished after $executionTime ms")
                }
            }
            job.join()
        } catch (e: Exception) {
            when (e) {
                is TimeoutCancellationException -> {
                    tra.results.add(TestResult(desc, e, TestResult.TestState.TIMEOUT))
                    logger.debug("Test ${desc.testCls}.${desc.name} execution TIMEOUT - allowed time $timeOut ms")
                }
            }
            throw e
        } finally {
            job!!.cancel()
        }
    }

    private class TestIdentifierListener : SummaryGeneratingListener() {
        private val identifiers: MutableList<TestIdentifier> = ArrayList()

        fun getIdentifiers(): List<TestIdentifier> {
            return unmodifiableList(identifiers)
        }

        override fun executionStarted(testIdentifier: TestIdentifier) {
            if (testIdentifier.isTest) {
                identifiers.add(testIdentifier)
            }
        }
    }

    companion object {
        private val launcher: Launcher = LauncherFactory.create()

        fun getTests(res: MutableList<TestItem>, item: Class<*>, executionTime: Long) {
            val listener = TestIdentifierListener()
            launcher.execute(
                LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(item))
                    .build(), listener
            )
            listener.getIdentifiers().map { res.add(Junit5TestItem(item, it, executionTime)) }
        }
    }
}