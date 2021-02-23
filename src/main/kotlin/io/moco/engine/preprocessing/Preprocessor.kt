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
                println("[MoCo] Error while executing test ${test?.testItem}")
            } finally {
            }
        }
    }
}