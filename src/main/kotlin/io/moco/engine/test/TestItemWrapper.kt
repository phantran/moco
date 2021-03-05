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

import io.moco.utils.MoCoLogger
import kotlinx.coroutines.*

class TestItemWrapper(val testItem: TestItem, val testResultAggregator: TestResultAggregator) {
    private val logger = MoCoLogger()

    suspend fun call() = withContext(Dispatchers.Default) {
        val timeOut: Long = if (testItem.executionTime != -1L) {
            // Normally tests executed in mutation tests phase will go into this branch
            // Because all tests with executionTime < 0  are filtered out (recorded from preprocessing step)
            (testItem.executionTime * 1.5 + 3000).toLong()
        } else if (configuredTestTimeOut != -1L) {
            configuredTestTimeOut
        } else {
            // If still no timeout can be calculated, use a default value of 10 seconds
            10000L
        }

        try {
            withTimeout(timeOut) {
                testItem.execute(testResultAggregator, timeOut)
            }
        } catch (ex: Exception) {
            logger.warn("Preprocessing: Error while executing test ${testItem.desc.name}")
            throw ex
        }
    }

    companion object {
        var configuredTestTimeOut: Long = -1

        fun wrapTestItem(testItems: List<TestItem>): Pair<List<TestItemWrapper>, List<TestResultAggregator>> {
            val wrappedItems: MutableList<TestItemWrapper> = mutableListOf()
            val aggregator: MutableList<TestResultAggregator> = mutableListOf()

            for (item: TestItem in testItems) {
                val resultAggregator = TestResultAggregator(mutableListOf())
                wrappedItems.add(TestItemWrapper(item, resultAggregator))
                aggregator.add(resultAggregator)
            }
            return Pair(wrappedItems, aggregator)
        }
    }
}