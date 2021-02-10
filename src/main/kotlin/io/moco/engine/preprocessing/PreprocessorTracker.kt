package io.moco.engine.preprocessing

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
        fun registerMappingTestToCUT(testClass: String) {
            if (testToCUTTracker.containsKey(testClass)) {
                testToCUTTracker[testClass]?.addAll(cutRecord)
            } else {
                testToCUTTracker[testClass] = cutRecord
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

        fun getPreprocessResults(): List<PreprocessClassResult> {
            val res: MutableList<PreprocessClassResult> = mutableListOf()
            for (cutCls: String in blockTracker.keys) {
                val recordTestClasses: MutableList<String> = mutableListOf()
                for (testCls: String in testToCUTTracker.keys) {
                    if (testToCUTTracker.get(testCls)?.contains(cutCls) == true) {
                        recordTestClasses.add(testCls)
                    }
                }
                res.add(PreprocessClassResult(recordTestClasses, cutCls, blockTracker.get(cutCls)))
            }
            return res
        }

    }
}