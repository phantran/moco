package io.moco.engine.tracker

import io.moco.engine.ClassInfo
import io.moco.engine.mutation.Mutation
import io.moco.engine.mutation.MutatedMethodLocation
import io.moco.engine.mutation.MutationID
import io.moco.engine.operator.Operator
import java.util.*

class MutatedMethodTracker (
    val mutatedClassTracker: MutatedClassTracker,
    val mutatedMethodLocation: MutatedMethodLocation
) {

    private var instructionIndex = 0
    private var currMutatedLineNumber = 0
    private val mutationFindingDisabledReasons: MutableSet<String> = HashSet()

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


    val classInfo: ClassInfo?
        get() = mutatedClassTracker.getClsInfo()

    fun mutantExists(newMutationID: MutationID): Boolean {
        return mutatedClassTracker.contextHasMutation(newMutationID)
    }

    fun disableMutations(reason: String) {
        mutationFindingDisabledReasons.add(reason)
    }

    fun enableMutatations(reason: String) {
        mutationFindingDisabledReasons.remove(reason)
    }

    fun increment() {
        instructionIndex += 1
    }

    fun currentInstructionCount(): Int {
        return instructionIndex
    }
}