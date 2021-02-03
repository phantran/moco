package io.moco.engine.tracker

data class Block (
    private val fstIns: Int,
    private val lstIns: Int,
    private val lines: MutableSet<Int?>,
) {
    fun getLines(): MutableSet<Int?> {
        return this.lines
    }

    fun getFstIns(): Int {
        return this.fstIns
    }

    fun getLstIns(): Int {
        return this.lstIns
    }
}