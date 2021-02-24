package io.moco.engine.test

import kotlinx.coroutines.*

class TestItemWrapper(val testItem: TestItem, val testResultAggregator: TestResultAggregator) {

    suspend fun call() = withContext(Dispatchers.Default) {
        val timeOut: Long = if (testItem.executionTime != -1L) {
            (testItem.executionTime * 1.5 + 5000).toLong()
        } else {
            configuredTestTimeOut
        }
        try {
            withTimeout(timeOut) {
                testItem.execute(testResultAggregator, timeOut)
            }
        } catch (ex: Exception) {
            println("[MoCo] Preprocessing: Error while executing test ${testItem.desc.name}")
        }
    }

    companion object {
        var configuredTestTimeOut: Long = 0

        fun wrapTestItem(testItems: List<TestItem>): Pair<List<TestItemWrapper>, List<TestResultAggregator>> {
            // Put test items into callable object so it can be submitted by thread executor service
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