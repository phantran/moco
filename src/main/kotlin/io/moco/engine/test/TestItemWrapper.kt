package io.moco.engine.test

import kotlinx.coroutines.*
import java.util.concurrent.Callable

class TestItemWrapper(val testItem: TestItem, val testResultAggregator: TestResultAggregator) : Callable<Unit> {
    override fun call() {
        println("Executing test " + testItem.desc)
        val t0 = System.currentTimeMillis()
        if (testItem.executionTime != -1L){
            val heuristicTimeOut = (testItem.executionTime * 1.5 + 5000).toLong()
            try {
                runBlocking {
                    withTimeout(heuristicTimeOut) {
                        testItem.execute(testResultAggregator)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                testResultAggregator.results.add(TestResult(testItem.desc,
                    e, TestResult.TestState.TIMEOUT))
                println("Test execution timeout - allowed time is $heuristicTimeOut")
                return
            }
        } else {
            testItem.execute(testResultAggregator)
            testItem.executionTime = System.currentTimeMillis() - t0
        }
        val executionTime = (System.currentTimeMillis() - t0).toInt()
        println("Execution time: $executionTime milliseconds")
    }


    companion object {
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