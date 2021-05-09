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


package io.github.phantran.engine.test

import io.github.phantran.engine.ClassName
import io.github.phantran.engine.preprocessing.PreprocessClassResult
import io.github.phantran.engine.preprocessing.PreprocessStorage

/**
 * Related test retriever
 *
 * @constructor
 *
 * @param buildRoot
 */
class RelatedTestRetriever(buildRoot: String) {

    private val store: PreprocessStorage = PreprocessStorage.getPreprocessStorage(buildRoot)!!

    /**
     * Retrieve related test
     *
     * This function retrieve all test classes that were associated to a class under test during the preprocessing
     * step (in preprocess json file). Test classes are ignored if its recorded execution time is -1.
     * An execution time of value -1 means the text was executed unsuccesfully in the preprocessing step due
     * to some reasons (not because of mutation introduction but because of some existing problems in the test suite
     *
     * @param cut
     * @return
     */
    fun retrieveRelatedTest(cut: ClassName, executionTimeInfo: Map<String, Long>): List<ClassName> {
        // TODO: DEPRECATED - NO LONGER USED AND TO BE REMOVED
        for (item: PreprocessClassResult in store.classRecord) {
            if (cut.name == item.classUnderTestName && !item.testClasses.isNullOrEmpty()) {
                // Filter test classes with execution time = -1 and transform to internal class name
                return item.testClasses.filter { executionTimeInfo[it] != -1L }
                    .map { ClassName(it.replace(".", "/")) }
            }
        }
        return listOf()
    }
}
