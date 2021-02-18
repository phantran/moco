package io.moco.engine.mutation


data class Mutant(
    val byteArr: Mutation,
    val byteArray: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mutant

        if (byteArr != other.byteArr) return false
        if (!byteArray.contentEquals(other.byteArray)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = byteArr.hashCode()
        result = 31 * result + byteArray.contentHashCode()
        return result
    }
}