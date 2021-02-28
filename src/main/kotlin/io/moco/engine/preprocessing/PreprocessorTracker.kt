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

        // Map from test class name to set of classes under test
        var testToCUTTracker: ConcurrentHashMap<String, MutableSet<String>> = ConcurrentHashMap()
        var lineTracker: ConcurrentHashMap<String, MutableSet<Int>> = ConcurrentHashMap()
        var cutRecord: MutableSet<String> = mutableSetOf()
        // Internal class name of ProcessorTracker to be called by asm method visitor
        val internalClsName: String = PreprocessorTracker::class.qualifiedName.toString().replace(".", "/")
        private var testExecutionTime = mutableMapOf<String, Long>()

        //        var blockTracker: ConcurrentHashMap<String, MutableList<Block>> = ConcurrentHashMap()

        @Synchronized
        @JvmStatic
        fun registerMappingTestToCUT(testClass: TestItem) {
            if (testToCUTTracker.containsKey(testClass.cls.name)) {
                testToCUTTracker[testClass.cls.name]?.addAll(cutRecord)
            } else {
                testToCUTTracker[testClass.cls.name] = cutRecord
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
        }

        fun getPreprocessResults(): PreprocessStorage {
            val res: MutableList<PreprocessClassResult> = mutableListOf()
            for (cutCls: String in lineTracker.keys) {
                val recordTestClasses: MutableList<CollectedTestInfo> = mutableListOf()
                for (testCls in testToCUTTracker.keys) {
                    if (testToCUTTracker[testCls]?.contains(cutCls) == true) {
                        val testInfo = CollectedTestInfo(testCls, testExecutionTime[testCls])
                        recordTestClasses.add(testInfo)
                    }
                }
                res.add(PreprocessClassResult(cutCls, recordTestClasses, lineTracker[cutCls]))
            }
            return PreprocessStorage(res)
        }
    }

        //        @Synchronized
//        @JvmStatic
//        fun registerBlock(className: String, blocks: List<Block>) {
//            if (blockTracker.containsKey(className)) {
//                blockTracker[className]?.addAll(blocks)
//            } else {
//                blockTracker[className] = blocks.toMutableList()
//            }
//        }
//        fun getPreprocessResults(): PreprocessStorage {
//            println(lineTracker)
//            println("here")
//            val res: MutableList<PreprocessClassResult> = mutableListOf()
//            for (cutCls: String in blockTracker.keys) {
//                val recordTestClasses: MutableList<Pair<String, Long?>> = mutableListOf()
//                for (testCls: String in testToCUTTracker.keys) {
//                    if (testToCUTTracker[testCls]?.contains(cutCls) == true) {
//                        val temp = Pair(testCls, testExecutionTime[testCls])
//                        recordTestClasses.add(temp)
//                    }
//                }
//                res.add(PreprocessClassResult(cutCls, recordTestClasses, blockTracker[cutCls]))
//            }
//            return PreprocessStorage(res)
//        }
}