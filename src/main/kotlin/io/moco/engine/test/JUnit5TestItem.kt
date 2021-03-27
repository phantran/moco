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


class JUnit5TestItem(cls: Class<*>, testIdentifier: Any, executionTime: Long = -1) :
    TestItem(cls, testIdentifier, executionTime) {

    override val desc = Description((testIdentifier as TestIdentifier).displayName, cls.name)

    override suspend fun execute(tra: TestResultAggregator, timeOut: Long) {
        var job: Job? = null
        try {
            val launcher = LauncherFactory.create()
            val launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(DiscoverySelectors.selectUniqueId((testIdentifier as TestIdentifier).uniqueId))
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
                    logger.debug("Test ${this@JUnit5TestItem} finished after $executionTime ms")
                }
            }
            job.join()
        } catch (e: Exception) {
            when (e) {
                is TimeoutCancellationException -> {
                    tra.results.add(TestResult(desc, e, TestResult.TestState.TIMEOUT))
                    logger.debug("Test ${this@JUnit5TestItem}  execution TIMEOUT - allowed time $timeOut ms")
                }
                else -> logger.info(e.printStackTrace().toString())
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

    override fun toString(): String {
        return ("${desc.testCls}.${desc.name}")
    }

    companion object {
        private val launcher: Launcher = LauncherFactory.create()
        private val cachedIdentifiers: MutableSet<TestIdentifier> = mutableSetOf()

        fun getTests(res: MutableList<TestItem>, item: Class<*>) {
            val listener = TestIdentifierListener()
            launcher.execute(
                LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(item))
                    .build(), listener
            )
            listener.getIdentifiers().map { res.add(JUnit5TestItem(item, it, -1)) }
        }

        fun getTestItem(cls: Class<*>, testIdentifier: Any, executionTime: Long): TestItem? {
            // Cache identifiers so that found identifiers that are executing and being stuck won't
            // make LauncherDiscoveryRequestBuilder falls in an infinite loop
            var foundIdentifier: TestIdentifier? = cachedIdentifiers.find { it.toString() == testIdentifier.toString() }
            if (foundIdentifier == null) {
                val listener = TestIdentifierListener()
                launcher.execute(
                    LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(cls))
                        .build(), listener
                )
                foundIdentifier = listener.getIdentifiers().find { it.toString() == testIdentifier }
                if (foundIdentifier != null) cachedIdentifiers.add(foundIdentifier)
            }
            return if (foundIdentifier != null) {
                JUnit5TestItem(cls, foundIdentifier, executionTime)
            } else null
        }
    }
}