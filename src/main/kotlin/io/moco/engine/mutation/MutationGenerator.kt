package io.moco.engine.mutation

import io.moco.engine.ClassName
import io.moco.engine.DefaultClassVisitor
import io.moco.engine.io.ByteArrayLoader
import io.moco.engine.operator.Operator
import io.moco.engine.tracker.MutatedClassTracker
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

/**
 * Mutation finder
 *
 * @property bytesArrayLoader
 * @property operators
 * @constructor Create empty Mutation finder
 */

class MutationGenerator(
    private val bytesArrayLoader: ByteArrayLoader,
    private val operators: List<Operator>?,
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
        val bytesArray: ByteArray? = bytesArrayLoader.getByteArray(
            clsToMutate.getInternalName()
        )
        if (bytesArray != null) {
            return visitAndCollectMutations(tracker, bytesArray)
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
     * Create actual mutant`
     *
     * @param mutation
     * @return
     */
    fun createMutant(mutationID: MutationID): Mutant {
        val tracker = MutatedClassTracker()
        val clsJavaName = mutationID.location.className?.getJavaName()
        val byteArray: ByteArray? = bytesArrayLoader.getByteArray(clsJavaName)
        val cr = ClassReader(byteArray)
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        val filter = listOf<String>()
        val mcv = MutatedClassVisitor(
            cw, tracker, filter, operators?.filter { it.getName() == mutationID.operatorName }
        )
        cr.accept(mcv, ClassReader.EXPAND_FRAMES)
        val mutations: List<Mutation> = tracker.getMutation(mutationID)
        return Mutant(mutations[0], cw.toByteArray())
    }
}