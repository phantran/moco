package io.moco.engine.preprocessing

import io.moco.engine.tracker.Block
import java.util.concurrent.ConcurrentHashMap


object PreprocessorTracker {
    //TODO: map from test class to list of CUT
    var testToCUTTracker: ConcurrentHashMap<String, MutableSet<String>> = ConcurrentHashMap()
    //TODO: map from CUT to list of blocks
    var blockTracker: ConcurrentHashMap<String, MutableList<Block>> = ConcurrentHashMap()
    var cutRecord: MutableSet<String> = mutableSetOf()


    @Synchronized
    fun registerBlock(className: String, blocks: List<Block>) {
        if (blockTracker.containsKey(className)) {
            blockTracker[className]?.addAll(blocks)
        } else {
            blockTracker[className] = blocks.toMutableList()
        }
    }

    @Synchronized
    fun registerMappingTestToCUT(testClass: String) {
        if (testToCUTTracker.containsKey(testClass)) {
            testToCUTTracker[testClass]?.addAll(cutRecord)
        } else {
            testToCUTTracker[testClass] = cutRecord
        }

        println("co vao day ko")
        println(testToCUTTracker)

    }

    @Synchronized
    fun registerCUT(cut: String) {
        cutRecord.add(cut)
    }

    @Synchronized
    fun clearTracker() {
        cutRecord = mutableSetOf()
    }
}