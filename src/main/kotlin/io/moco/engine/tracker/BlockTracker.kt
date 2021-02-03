package io.moco.engine.tracker


class BlockTracker  {
    var currBlockOfClass = 0
        private set
    var currBlockOfMethod = 0
        private set
    var isWithinFinallyBlock = false
        private set

    fun detectBlock() {
        currBlockOfClass++
        currBlockOfMethod++
    }

    fun detectFinallyStart() {
        isWithinFinallyBlock = true
    }

    fun detectFinallyEnd() {
        isWithinFinallyBlock = false
    }

    fun detectStartMethod() {
        currBlockOfMethod = 0
    }
}
