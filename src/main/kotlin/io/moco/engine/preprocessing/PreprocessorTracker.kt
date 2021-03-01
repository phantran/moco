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

import io.moco.engine.test.TestItem
import java.util.concurrent.ConcurrentHashMap


class PreprocessorTracker {
    companion object {
        // map from class name -> set of covered lines (corresponding to a run of a test class)
        var lineTracker: ConcurrentHashMap<String, MutableSet<Int>> = ConcurrentHashMap()
        var cutRecord: MutableSet<String> = mutableSetOf()
        val cutToLines: MutableMap<String, MutableSet<Int>> = mutableMapOf()
        val cutToTest: MutableMap<String, MutableSet<String>> = mutableMapOf()
        // Internal class name of ProcessorTracker to be called by asm method visitor
        private var testExecutionTime = mutableMapOf<String, Long>()
        val internalClsName: String = PreprocessorTracker::class.qualifiedName.toString().replace(".", "/")
        var previousRemainingTests: List<String?> = mutableListOf()
        var errorTests: MutableList<String?> = mutableListOf()


        @Synchronized
        @JvmStatic
        fun registerMappingCutToTestInfo(testClass: TestItem) {
            for (item in lineTracker.keys()) {
                if (cutToLines.keys.contains(item)) {
                    cutToLines[item]?.addAll(lineTracker[item]!!)
                } else {
                    cutToLines[item] = lineTracker[item]!!
                }
            }
            for (item in cutRecord) {
                // cutRecord contains a class name means a test has invoked a method of that class
                if (!cutToTest.keys.contains(item)) {
                    cutToTest[item] = mutableSetOf(testClass.cls.name)
                } else {
                    cutToTest[item]?.add(testClass.cls.name)
                }
            }
            if (testExecutionTime.containsKey(testClass.cls.name)) {
                if (testClass.executionTime > testExecutionTime[testClass.cls.name]!!) {
                    testExecutionTime[testClass.cls.name] = testClass.executionTime
                }
            } else {
                testExecutionTime[testClass.cls.name] = testClass.executionTime
            }
        }

        @Synchronized
        @JvmStatic
        fun registerCUT(cut: String) {
            cutRecord.add(cut)
        }

        @Synchronized
        @JvmStatic
        fun registerLine(className: String, hitLine: Int) {
            if (lineTracker.containsKey(className)) {
                lineTracker[className]?.add(hitLine)
            } else {
                lineTracker[className] = mutableSetOf(hitLine)
            }
        }

        @Synchronized
        @JvmStatic
        fun clearTracker() {
            cutRecord = mutableSetOf()
            lineTracker = ConcurrentHashMap()
        }

        fun getPreprocessResults(): PreprocessStorage {
            val classRecord = mutableListOf<PreprocessClassResult>()
            for (item in cutToTest.keys) {
                if (cutToLines.keys.contains(item)) {
                    val testClasses = cutToTest[item]
                    val coveredLines = cutToLines[item]
                    classRecord.add(PreprocessClassResult(item, testClasses!!, coveredLines))
                } else {
                    continue
                }
            }
            return PreprocessStorage(classRecord, testExecutionTime, previousRemainingTests, errorTests)
        }
    }
}