package io.moco.engine.mutation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.io.Serializable

data class Mutation (
    @JsonUnwrapped val mutationID: MutationID,
    @JsonProperty("fileName") val fileName: String? = "unknown_source_file",
    @JsonProperty("loc") val lineOfCode: Int,  // Line number of the mutant
    @JsonProperty("mutationDescription")val description: String,  // Additional information about the mutant
    //TODO: Add feature for find finally block and static initializer later
) : Serializable