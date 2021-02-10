package io.moco.engine.preprocessing

import io.moco.engine.ClassName
import io.moco.engine.test.TestItem
import io.moco.engine.test.TestItemWrapper
import io.moco.engine.test.TestResultAggregator
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors


class Preprocessor(
    private val codeBase: Codebase,
) {
    /**
    This class is responsible for parsing the whole codebase to get the information of
    which test classes are responsible for which classes under test and store this information
    in an XML file. This file will be read in each execution of Gamekins to retrieve this mapping information
     */

    fun preprocessing() {
        // Codebase is already available
        val testItems = testClassesToTestItems()
        collectInfo(wrapTestItem(testItems))
    }

    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    private fun collectInfo(
        wrappedTests: Collection<TestItemWrapper?>,
    ) {
        for (test: TestItemWrapper? in wrappedTests) {
            try {
                if (test != null) {
                    test.call()
                    PreprocessorTracker.registerMappingTestToCUT(test.testItem.cls.name)
                }
            } catch (e: Exception) {
                println("Error while executing test ${test?.testItem}")
            } finally {
                PreprocessorTracker.clearTracker()
            }
        }
    }

    private fun testClassesToTestItems(): List<TestItem?> {
        // convert from test classes to test items so it can be executed
        return codeBase.testClassesNames.map { it?.let { ClassName.clsNameToClass(it)?.let { it1 -> TestItem(it1) } } }
            .toList()
    }

    private fun wrapTestItem(testItems: List<TestItem?>): List<TestItemWrapper?> {
        // Put test items into callable object so it can be submitted by thread executor service
        return testItems.map { it?.let { TestItemWrapper(it, TestResultAggregator(mutableListOf())) } }.toList()

    }
}