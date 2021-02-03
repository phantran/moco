package io.moco.engine.tracker


class InstructionTracker {
    private var count = 0
    fun increment() {
        count++
    }

    fun currentInstructionCount(): Int {
        return count
    }
}
