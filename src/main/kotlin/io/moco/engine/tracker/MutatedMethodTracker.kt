package io.moco.engine.tracker

import io.moco.engine.mutation.Mutation
import io.moco.engine.mutation.MutatedMethodLocation
import io.moco.engine.mutation.MutationID
import io.moco.engine.operator.Operator

class MutatedMethodTracker(
    private val mutatedClassTracker: MutatedClassTracker,
    private val mutatedMethodLocation: MutatedMethodLocation
) {

    var instructionIndex = 0
    var currMutatedLineNumber = 0

    fun registerMutant(
        operator: Operator, description: String
    ): MutationID {
        val newMutationID = MutationID(mutatedMethodLocation, mutableListOf(instructionIndex), operator.getName())
        val newMutant = Mutation(
            newMutationID,
            mutatedClassTracker.getFileName(),
            currMutatedLineNumber,
            description,
        )
        if (mutatedClassTracker.targetMutationID == null) {
            // target mutation id is null means it is in mutation collecting state
            this.mutatedClassTracker.addMutation(newMutant)
        } else {
            // target mutation id is not null, we only care about the target mutation
            this.mutatedClassTracker.setTargetMutation(newMutant)
        }
        return newMutationID
    }


    fun isTargetMutation(newMutationID: MutationID): Boolean {
        return newMutationID == mutatedClassTracker.targetMutationID
    }
}