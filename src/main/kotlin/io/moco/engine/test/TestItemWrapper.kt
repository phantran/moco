package io.moco.engine.test

import io.moco.utils.MoCoLogger
import kotlinx.coroutines.*

class TestItemWrapper(val testItem: TestItem, val testResultAggregator: TestResultAggregator) {
    private val logger = MoCoLogger()

    suspend fun call() = withContext(Dispatchers.Default) {
        var timeOut: Long = if (testItem.executionTime != -1L) {
            (testItem.executionTime * 1.5 + 5000).toLong()
        } else {
            configuredTestTimeOut
        }
        if (timeOut == -1L) {
            // If still no timeout can be calculated, use a default value of 20 seconds
            timeOut = 30000L
        }
        try {
            withTimeout(timeOut) {
                testItem.execute(testResultAggregator, timeOut)
            }
        } catch (ex: Exception) {
            logger.warn("Preprocessing: Error while executing test ${testItem.desc.name}")
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