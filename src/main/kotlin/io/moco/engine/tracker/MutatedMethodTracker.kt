package io.moco.engine.tracker

import io.moco.engine.ClassInfo
import io.moco.engine.Mutant
import io.moco.engine.MutatedMethodLocation
import io.moco.engine.operator.Operator
import java.util.*

class MutatedMethodTracker (
    val mutatedClassTracker: MutatedClassTracker,
    val mutatedMethodLocation: MutatedMethodLocation) {

    private var instructionIndex = 0
    private var currMutatedLineNumber = 0
    private val mutationFindingDisabledReasons: MutableSet<String> = HashSet()

    fun registerMutant(
        operator: Operator, description: String
    ): String {
        val newMutantID = UUID.randomUUID().toString()
        val newMutant = Mutant(
            newMutantID,
            mutatedClassTracker.getFileName(),
            currMutatedLineNumber,
            description,
            mutatedMethodLocation,
            mutableListOf(instructionIndex),
            operator.getName(),
//            mutatedClassTracker.block,
            )
        this.mutatedClassTracker.addMutation(newMutant)
        return newMutantID
    }


    val classInfo: ClassInfo?
        get() = mutatedClassTracker.getClsInfo()

    fun mutantExists(newMutantId: String): Boolean {
        return mutatedClassTracker.contextHasMutation(newMutantId)
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