package io.moco.engine.mutation

import io.moco.engine.ClassName
import io.moco.engine.DefaultClassVisitor
import io.moco.engine.io.ByteArrayLoader
import io.moco.engine.operator.Operator
import io.moco.engine.tracker.MutatedClassTracker
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter

/**
 * Mutation finder
 *
 * @property bytesArrayLoader
 * @property operators
 * @constructor Create empty Mutation finder
 */

class MutationGenerator(
    val bytesArrayLoader: ByteArrayLoader,
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
            clsToMutate.name
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
     * @param mutationID
     * @return
     */
    fun createMutant(mutationID: MutationID, byteArray: ByteArray?): Mutant? {
        val tracker = MutatedClassTracker(mutationID)
        val cr = ClassReader(byteArray)
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
//        val cv = TraceClassVisitor(cw, PrintWriter(System.out))

        val filter = listOf<String>()
        val mcv = MutatedClassVisitor(
            cw, tracker, filter, operators?.filter { it.getName() == mutationID.operatorName }
        )
        cr.accept(mcv, ClassReader.EXPAND_FRAMES)

        return if (tracker.getTargetMutation() != null) {
            Mutant(tracker.getTargetMutation()!!, cw.toByteArray())
        } else {
            null
        }
    }
}