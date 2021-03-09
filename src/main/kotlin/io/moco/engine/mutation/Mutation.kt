/*
 * Copyright (c) 2021. Tran Phan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.moco.engine.mutation

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.io.Serializable

data class Mutation (
    @JsonUnwrapped val mutationID: MutationID,
    @JsonProperty("fileName") val fileName: String? = "unknown_source_file",
    @JsonProperty("loc") val lineOfCode: Int,  // Line number of the mutant
    @JsonProperty("mutationDescription")val description: String,  // Additional information about the mutant
    @JsonIgnore var varIndicesInLoop: MutableMap<String, Any?>? = mutableMapOf(),
    @JsonProperty("instructionsOrder") var instructionsOrder: MutableList<String> = mutableListOf(),  // Additional information about the mutant
    @JsonProperty("additionalInfo") var additionalInfo: MutableMap<String, String> = mutableMapOf(),  // Additional information about the mutant
    //TODO: Add feature for find finally block and static initializer later
) : Serializable