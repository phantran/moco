package io.moco.engine.preprocessing

import io.moco.engine.Codebase
import io.moco.engine.test.TestItem
import io.moco.engine.test.TestItemWrapper
import java.io.IOException
import java.util.concurrent.ExecutionException



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
        val testItems = TestItem.testClassesToTestItems(codeBase.testClassesNames)
        val wrapped = TestItemWrapper.wrapTestItem(testItems)
        collectInfo(wrapped.first)
    }

    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    private fun collectInfo(
        wrappedTests: Collection<TestItemWrapper?>,
    ) {
        for (test: TestItemWrapper? in wrappedTests) {
            try {
                if (test != null) {
                    test.call()
                    PreprocessorTracker.registerMappingTestToCUT(test.testItem)
                    PreprocessorTracker.clearTracker()
                }
            } catch (e: Exception) {
                println("Error while executing test ${test?.testItem}")
            } finally {
            }
        }
    }
}