package io.moco.engine.tracker

import io.moco.engine.ClassInfo
import io.moco.engine.mutation.Mutant


class MutatedClassTracker {
    private var consideredMutantID: String? = null
    private var mutations: MutableList<Mutant> = mutableListOf()
    private var clsInfo: ClassInfo? = null
    private var fileName: String? = null

    val javaClsName: String?
        get() = clsInfo?.name?.replace("/", ".")

    fun getMutation(mutantID: String): List<Mutant> {
        return mutations.filter{ it.id == mutantID }
    }

    fun contextHasMutation(newMutantID: String): Boolean {
        return mutations.any{ it.id == newMutantID }
    }

    fun addMutation(mutation: Mutant) {
        mutations.add(mutation)
    }

    fun getCollectedMutations(): List<Mutant> {
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