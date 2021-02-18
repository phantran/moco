package io.moco.engine.mutation

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class MutationID(
    @JsonProperty("methodInfo") val location: MutatedMethodLocation,
    @JsonProperty("instructionIndices") val instructionIndices: Collection<Int>?,
    @JsonProperty("mutationOperatorName") val operatorName: String  // mutation operator unique name
): Serializable