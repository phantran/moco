package io.moco.engine.tracker


data class Block (
    val fstIns: Int,
    val lstIns: Int,
    val lines: MutableSet<Int?>,
) {
}