package io.moco.engine

/**
 * Mutation Generator to create mutants.
 *
 * @author Tran Phan
 * @since 1.0
 */
class DummyForTesting() {
    fun dummy() {
        val a = 1
        val b = 5 + 4
        val c = 10 / 2
        val d = 2 * 4
        val e = a + b
        if (e == 10) {
            val f = 4 - 9
        }
    }
}