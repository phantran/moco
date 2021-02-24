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
import io.moco.engine.tracker.Block
import java.util.concurrent.ConcurrentHashMap


class PreprocessorTracker {
    companion object {

        var testToCUTTracker: ConcurrentHashMap<String, MutableSet<String>> = ConcurrentHashMap()
        var blockTracker: ConcurrentHashMap<String, MutableList<Block>> = ConcurrentHashMap()
        var cutRecord: MutableSet<String> = mutableSetOf()

        // Internal class name of ProcessorTracker to be called by asm method visitor
        val internalClsName: String = PreprocessorTracker::class.qualifiedName.toString().replace(".", "/")
        private var testExecutionTime = mutableMapOf<String, Long>()

        @Synchronized
        @JvmStatic
        fun registerBlock(className: String, blocks: List<Block>) {
            if (blockTracker.containsKey(className)) {
                blockTracker[className]?.addAll(blocks)
            } else {
                blockTracker[className] = blocks.toMutableList()
            }
        }

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
        fun clearTracker() {
            cutRecord = mutableSetOf()
        }

        fun getPreprocessResults(): PreprocessStorage {
            val res: MutableList<PreprocessClassResult> = mutableListOf()
            for (cutCls: String in blockTracker.keys) {
                val recordTestClasses: MutableList<Pair<String, Long?>> = mutableListOf()
                for (testCls: String in testToCUTTracker.keys) {
                    if (testToCUTTracker[testCls]?.contains(cutCls) == true) {
                        val temp = Pair(testCls, testExecutionTime[testCls])
                        recordTestClasses.add(temp)
                    }
                }
                res.add(PreprocessClassResult(cutCls, recordTestClasses, blockTracker[cutCls]))
            }
            return PreprocessStorage(res)
        }
    }
}