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

        // {"className1 to ["lineOfCode" to set{testClasses...}], "className2 to ["lineOfCode" to set{testClasses...}]}
        val cutToLineTestMap: MutableMap<String, MutableMap<Int, MutableSet<String>>> = mutableMapOf()
        val cutToTest: MutableMap<String, MutableSet<String>> = mutableMapOf()

        // Internal class name of ProcessorTracker to be called by asm method visitor
        private var testExecutionTime = mutableMapOf<String, Long>()
        val internalClsName: String = PreprocessorTracker::class.qualifiedName.toString().replace(".", "/")
        var previousRemainingTests: List<String?> = mutableListOf()
        var errorTests: MutableList<String?> = mutableListOf()


        @Synchronized
        @JvmStatic
        fun registerMappingCutToTestInfo(testClass: TestItem) {
            for ((cut, lines) in lineTracker.entries) {
                if (cutToLineTestMap.keys.contains(cut)) {
                    lines.map {
                        if (cutToLineTestMap[cut]!!.containsKey(it)) {
                            cutToLineTestMap[cut]!![it]?.add(testClass.cls.name)
                        } else cutToLineTestMap[cut]!!.set(it, mutableSetOf(testClass.cls.name))
                    }
                } else {
                    cutToLineTestMap[cut] = mutableMapOf()
                    lines.map { cutToLineTestMap[cut]!![it] = mutableSetOf(testClass.cls.name) }
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
            for (cut in cutToTest.keys) {
                if (cutToLineTestMap.keys.contains(cut)) {
                    val testClasses = cutToTest[cut]
                    val coveredLines = cutToLineTestMap[cut]
                    classRecord.add(PreprocessClassResult(cut, testClasses!!, coveredLines))
                } else {
                    continue
                }
            }
            return PreprocessStorage(classRecord, testExecutionTime, previousRemainingTests, errorTests)
        }
    }
}