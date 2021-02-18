package io.moco.engine.tracker

import io.moco.engine.ClassInfo
import io.moco.engine.mutation.Mutant
import io.moco.engine.mutation.Mutation
import io.moco.engine.mutation.MutationID


class MutatedClassTracker {
    private var consideredMutantID: String? = null
    private var mutations: MutableList<Mutation> = mutableListOf()
    private var clsInfo: ClassInfo? = null
    private var fileName: String? = null

    fun getMutation(mutationID: MutationID): List<Mutation> {
        return mutations.filter{ it.mutationID == mutationID }
    }

    fun isRegisteredMutant(newMutationID: MutationID): Boolean {
        return mutations.any{ it.mutationID == newMutationID }
    }

    fun addMutation(mutation: Mutation) {
        mutations.add(mutation)
    }

    fun getCollectedMutations(): List<Mutation> {
        return mutations
    }

    fun setConsideredMutantID(mutantID: String) {
        consideredMutantID = mutantID
    }

    fun getConsideredMutantID(): String? {
        return consideredMutantID
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