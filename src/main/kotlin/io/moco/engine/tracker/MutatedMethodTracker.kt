package io.moco.engine.tracker

import io.moco.engine.mutation.Mutation
import io.moco.engine.mutation.MutatedMethodLocation
import io.moco.engine.mutation.MutationID
import io.moco.engine.operator.Operator

class MutatedMethodTracker (
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
        this.mutatedClassTracker.addMutation(newMutant)
        return newMutationID
    }

    fun mutantExists(newMutationID: MutationID): Boolean {
        return mutatedClassTracker.contextHasMutation(newMutationID)
    }
}