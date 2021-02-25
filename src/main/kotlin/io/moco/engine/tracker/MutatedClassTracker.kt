package io.moco.engine.tracker

import io.moco.engine.ClassInfo
import io.moco.engine.mutation.Mutation
import io.moco.engine.mutation.MutationID


class MutatedClassTracker(val targetMutationID: MutationID? = null) {
    private var mutations: MutableList<Mutation> = mutableListOf()
    private var targetMutation: Mutation? = null
    private var clsInfo: ClassInfo? = null
    private var fileName: String? = null

    fun addMutation(mutation: Mutation) {
        mutations.add(mutation)
    }

    fun mutationAlreadyCollected(id: MutationID): Boolean {
        return mutations.any { it.mutationID == id }
    }

    fun getCollectedMutations(): List<Mutation> {
        return mutations
    }

    fun setTargetMutation(mutation: Mutation) {
        targetMutation = mutation
    }

    fun getTargetMutation(): Mutation? {
        return targetMutation
    }

    fun setClsInfo(info: ClassInfo) {
        clsInfo = info
    }

    fun getClsInfo(): ClassInfo? {
        return clsInfo
    }

    fun setFileName(name: String) {
        fileName = name
    }

    fun getFileName(): String? {
        return fileName
    }

}
