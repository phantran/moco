package io.moco.engine.mutation

import java.io.Serializable

data class MutationID(
    val location: MutatedMethodLocation,
    val instructionIndices: Collection<Int>?,
    val operatorName: String
): Serializable {
}