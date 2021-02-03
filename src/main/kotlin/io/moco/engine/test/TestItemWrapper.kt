package io.moco.engine.test


import java.util.concurrent.Callable

class TestItemWrapper(val testItem: TestItem, val testResultAggregator: TestResultAggregator): Callable<Unit> {
    override fun call() {
        println("Preprocess by executing test " + testItem.desc)
        val t0 = System.currentTimeMillis()
        this.testItem.execute(testResultAggregator)
        val executionTime = (System.currentTimeMillis() - t0).toInt()
        println(executionTime)
    }
}