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

class MutatedMethodTracker(
    val mutatedClassTracker: MutatedClassTracker,
    val mutatedMethodLocation: MutatedMethodLocation
) {

    var instructionIndex = 0
    var currConsideredLineNumber = 0

    fun registerMutation(
        operator: Operator, description: String, mutatorUniqueID: String
    ): Mutation? {
        val newMutationID = MutationID(mutatedMethodLocation, mutableListOf(instructionIndex),
                                       operator.operatorName, mutatorUniqueID)
        if (mutatedClassTracker.mutationAlreadyCollected(newMutationID)) {
            return null
        }
        if (mutatedMethodLocation.methodName.name == "<init>" || mutatedMethodLocation.methodName.name == "<clinit>") {
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

    fun isTargetMutation(newMutationID: MutationID): Boolean {
        return newMutationID == mutatedClassTracker.targetMutationID
    }
}