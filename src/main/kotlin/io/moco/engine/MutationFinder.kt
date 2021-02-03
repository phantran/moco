package io.moco.engine


import org.gamekins.mutation.io.BytecodeLoader
import org.gamekins.mutation.operator.Operator
import org.gamekins.mutation.tracker.MutatedClassTracker
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
    val bytecodeLoader: BytecodeLoader,
    val operators: List<Operator>?
) {
    /**
     * Find possible mutations
     *
     * @param clsToMutate
     * @return
     *///TODO: add filter as a property of this class to excluded classes and functions specify by users
    fun findPossibleMutations(
        clsToMutate: ClassName
    ): List<Mutant?> {
        val tracker = MutatedClassTracker()
        val byteArray: ByteArray? = bytecodeLoader.getByteCodeArray(
            clsToMutate.getInternalName()
        )
        if (byteArray != null) {
            return visitAndCollectMutations(tracker, byteArray)
        }
        return listOf<Mutant>()
    }


    private fun visitAndCollectMutations(
        tracker: MutatedClassTracker, clsToMutate: ByteArray
    ): List<Mutant?> {
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
    fun retrieveMutant(mutation: Mutant): Mutant {
        val tracker = MutatedClassTracker()
        val clsJavaName = mutation.mutatedMethodLocation.className?.getJavaName()
        val byteArray: ByteArray? = bytecodeLoader.getByteCodeArray(clsJavaName)
        val cr = ClassReader(byteArray)
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        val filter = listOf<String>()
        val mcv = MutatedClassVisitor(
            cw, tracker, filter, operators?.filter { it.getName() == mutation.operatorName }
        )

        cr.accept(mcv, ClassReader.EXPAND_FRAMES)

        val mutantsList: List<Mutant> = tracker.getMutation(mutation.id)
        mutantsList[0].setByteCode(cw.toByteArray())
        return mutantsList[0]
    }
}