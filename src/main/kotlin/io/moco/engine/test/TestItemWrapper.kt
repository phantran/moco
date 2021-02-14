package io.moco.engine.test


import java.util.concurrent.Callable

class TestItemWrapper(val testItem: TestItem, private val testResultAggregator: TestResultAggregator) : Callable<Unit> {
    override fun call() {
        println("Preprocess by executing test " + testItem.desc)
        val t0 = System.currentTimeMillis()
        this.testItem.execute(testResultAggregator)
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