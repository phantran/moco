package io.moco.engine.mutation

import io.moco.engine.ClassName
import io.moco.engine.DefaultClassVisitor
import io.moco.engine.io.BytecodeLoader
import io.moco.engine.operator.Operator
import io.moco.engine.tracker.MutatedClassTracker
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

/**
 * Mutation finder
 *
 * @property bytecodeLoader
 * @property operators
 * @constructor Create empty Mutation finder
 */

class MutationFinder(
    private val bytecodeLoader: BytecodeLoader,
    private val operators: List<Operator>?
) {
    /**
     * Find possible mutations
     *
     * @param clsToMutate
     * @return
     *///TODO: add filter as a property of this class to excluded classes and functions specify by users
    fun findPossibleMutationsOfClass(
        clsToMutate: ClassName
    ): List<Mutation> {
        val tracker = MutatedClassTracker()
        val byteArray: ByteArray? = bytecodeLoader.getByteCodeArray(
            clsToMutate.getInternalName()
        )
        if (byteArray != null) {
            return visitAndCollectMutations(tracker, byteArray)
        }
        return listOf()
    }


    private fun visitAndCollectMutations(
        tracker: MutatedClassTracker, clsToMutate: ByteArray
    ): List<Mutation> {
        val cr = ClassReader(clsToMutate)
        val filter = listOf<String>()
        val mcv = MutatedClassVisitor(DefaultClassVisitor(), tracker, filter, operators)
        cr.accept(mcv, ClassReader.EXPAND_FRAMES)
        return tracker.getCollectedMutations()
    }


    /**
     * Retrieve mutant
     *
     * @param mutation
     * @return
     */
    fun retrieveMutant(mutation: Mutation): Mutation {
        val tracker = MutatedClassTracker()
        val clsJavaName = mutation.mutationID.location.className?.getJavaName()
        val byteArray: ByteArray? = bytecodeLoader.getByteCodeArray(clsJavaName)
        val cr = ClassReader(byteArray)
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        val filter = listOf<String>()
        val mcv = MutatedClassVisitor(
            cw, tracker, filter, operators?.filter { it.getName() == mutation.mutationID.operatorName }
        )

        cr.accept(mcv, ClassReader.EXPAND_FRAMES)

        val mutantsList: List<Mutation> = tracker.getMutation(mutation.mutationID)
        mutantsList[0].setByteCode(cw.toByteArray())
        return mutantsList[0]
    }
}