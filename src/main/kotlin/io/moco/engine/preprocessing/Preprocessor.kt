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

import io.moco.engine.ClassName
import io.moco.engine.test.TestItem
import io.moco.engine.test.TestItemWrapper
import io.moco.persistence.JsonSource
import io.moco.utils.MoCoLogger
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.Exception
import java.lang.RuntimeException
import java.util.concurrent.ExecutionException


class Preprocessor(
    private val toBeRunTests: List<ClassName>,
) {

    /**
    This class is responsible for parsing the whole codebase to get the information of
    which test classes are responsible for which classes under test and store this information
    in an XML file. This file will be read in each execution of Gamekins to retrieve this mapping information
     */

    @Throws(IOException::class, InterruptedException::class, ExecutionException::class, RuntimeException::class)
    fun preprocessing(isRerun: Boolean = false, jsonConverter: JsonSource) {
        // Codebase is already available
        val testItems = TestItem.testClassesToTestItems(toBeRunTests)
        val wrapped = TestItemWrapper.wrapTestItem(testItems)
        if (isRerun) {
            val recoveredResult = jsonConverter.getData(PreprocessStorage::class.java) as PreprocessStorage
            val remainingTests = recoveredResult.previousRemainingTests
            if (remainingTests.isNullOrEmpty()) {
                return
            }
            PreprocessorTracker.errorTests = recoveredResult.errorTests
            logger.debug("Preprocessing: ${remainingTests.size} test class(es) to run after filtering")
            collectInfo(wrapped.first, isRerun, remainingTests)
        } else {
            logger.debug("Preprocessing: ${wrapped.first.size} test class(es) to run after filtering")
            collectInfo(wrapped.first)
        }
    }

    companion object {
        private val logger = MoCoLogger()

        @Throws(IOException::class, InterruptedException::class, ExecutionException::class, RuntimeException::class)
        fun collectInfo(
            wrappedTests: List<TestItemWrapper?>, isRerun: Boolean = false, remainingTests: List<String?> = listOf()
        ) {

            runBlocking {
                for (i in wrappedTests.indices) {
                    val testItem = wrappedTests.elementAt(i)
                    if (isRerun && !remainingTests.contains(testItem?.testItem?.cls?.name)) {
                        continue
                    }
                    PreprocessorTracker.clearTracker()
                    try {
                        testItem?.call()
                        if (testItem?.testResultAggregator?.results?.any { it.error is AssertionError } == true) {
                            logger.error("Ignore tests in ${testItem.testItem.desc.name} as it " +
                                            "contains failed test case(s), please fix it.")
                        } else {
                            PreprocessorTracker.registerMappingCutToTestInfo(testItem!!.testItem)
                            PreprocessorTracker.clearTracker()
                        }
                    } catch (e: Exception) {
                        if (i == wrappedTests.size - 1) {
                            return@runBlocking
                        } else {
                            val temp = wrappedTests.subList(i + 1, wrappedTests.size)
                            PreprocessorTracker.previousRemainingTests = temp.map { it?.testItem?.cls?.name }
                            testItem?.testItem?.cls?.name?.let { PreprocessorTracker.errorTests.add(it) }
                            throw Exception(i.toString())
                        }
                    }
                }
            }
            PreprocessorTracker.previousRemainingTests = listOf()
        }
    }
}