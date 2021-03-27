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

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

/**
 * Mutation i d
 *
 * @property location
 * @property instructionIndices
 * @property operatorName
 * @property mutatorID
 * @constructor Create empty Mutation i d
 */
data class MutationID(
    // IMPORTANT: Do not change json property names
    @JsonProperty("methodInfo") val location: MutatedMethodLocation,
    @JsonProperty("instructionIndices") val instructionIndices: Collection<Int>?,
    @JsonProperty("mutationOperatorName") val operatorName: String,
    @JsonProperty("mutatorID") var mutatorID: String
): Serializable {

    /**
     * Compare without instruction
     *
     * @param other
     * @return
     */
    fun compareWithoutInstruction(other: MutationID): Boolean {
        if (location.methodName == other.location.methodName &&
            location.className == other.location.className &&
            location.methodDesc == other.location.methodDesc &&
            mutatorID == other.mutatorID &&
            operatorName == other.operatorName) {
            return true
        }
        return false
    }
}