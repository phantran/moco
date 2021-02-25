package io.moco.engine.tracker

import io.moco.engine.mutation.Mutation
import io.moco.engine.mutation.MutatedMethodLocation
import io.moco.engine.mutation.MutationID
import io.moco.engine.operator.Operator

class MutatedMethodTracker(
    val mutatedClassTracker: MutatedClassTracker,
    private val mutatedMethodLocation: MutatedMethodLocation
) {

    var instructionIndex = 0
    var currConsideredLineNumber = 0

    fun registerMutation(
        operator: Operator, description: String
    ): Mutation {
        val newMutationID = MutationID(mutatedMethodLocation, mutableListOf(instructionIndex), operator.getName())
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