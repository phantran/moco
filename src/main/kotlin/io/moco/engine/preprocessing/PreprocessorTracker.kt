package io.moco.engine.preprocessing

import io.moco.engine.test.TestItem
import io.moco.engine.tracker.Block
import java.util.concurrent.ConcurrentHashMap


class PreprocessorTracker {
    companion object {
        //TODO: map from test class to list of CUT
        var testToCUTTracker: ConcurrentHashMap<String, MutableSet<String>> = ConcurrentHashMap()

        //TODO: map from CUT to list of blocks
        var blockTracker: ConcurrentHashMap<String, MutableList<Block>> = ConcurrentHashMap()
        var cutRecord: MutableSet<String> = mutableSetOf()

        // Internal class name of ProcessorTracker to be called by asm method visitor
        val internalClsName: String = PreprocessorTracker::class.qualifiedName.toString().replace(".", "/")
        private val testExecutionTime = mutableMapOf<String, Long>()

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