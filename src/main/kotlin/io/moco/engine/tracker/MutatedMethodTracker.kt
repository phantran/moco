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


package io.moco.engine.tracker

import io.moco.engine.mutation.Mutation
import io.moco.engine.mutation.MutatedMethodLocation
import io.moco.engine.mutation.MutationID
import io.moco.engine.operator.Operator

/**
 * Mutated method tracker
 *
 * @property mutatedClassTracker
 * @property mutatedMethodLocation
 * @constructor Create empty Mutated method tracker
 */
class MutatedMethodTracker(
    val mutatedClassTracker: MutatedClassTracker,
    val mutatedMethodLocation: MutatedMethodLocation
) {

    var instructionIndex = 0
    var currConsideredLineNumber = 0

    /**
     * Register mutation
     * This method record a possible mutation to mutated class tracker which would be a part of
     * the output of this mutation collecting step
     * @param operator
     * @param description
     * @param mutatorUniqueID
     * @return
     */
    fun registerMutation(
        operator: Operator, description: String, mutatorUniqueID: String
    ): Mutation? {
        val newMutationID = MutationID(mutatedMethodLocation, mutableListOf(instructionIndex),
                                       operator.operatorName, mutatorUniqueID)

        // MoCo does not collect mutations on a line that is uncovered by the tests.
        if (!mutatedClassTracker.coveredLines.isNullOrEmpty()) {
            if (!mutatedClassTracker.coveredLines.contains(currConsideredLineNumber)) {
                return null
            }
        }
        // Only collect unique mutation (mutation id must be unique)
        if (mutatedClassTracker.mutationAlreadyCollected(newMutationID)) {
            return null
        }
        // MoCo does not collect mutations in static init block
        if (mutatedMethodLocation.methodName.name == "<clinit>") {
            return null
        }
        val newMutation = Mutation(
            newMutationID,
            mutatedClassTracker.getFileName(),
            currConsideredLineNumber,
            description,
        )
        this.mutatedClassTracker.addMutation(newMutation)
        return newMutation
    }

    /**
     * Is target mutation
     *
     * @param newMutationID
     * @return
     */
    fun isTargetMutation(newMutation: Mutation): Boolean {
        return newMutation == mutatedClassTracker.targetMutation!!
    }
}