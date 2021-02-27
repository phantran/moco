/*
 * Copyright (c) 2021. Tran Phan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.moco.engine.mutation

import io.moco.engine.ClassName
import io.moco.engine.DefaultClassVisitor
import io.moco.engine.io.ByteArrayLoader
import io.moco.engine.operator.Operator
import io.moco.engine.tracker.MutatedClassTracker
import io.moco.utils.MoCoLogger
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.lang.Exception

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
    val logger = MoCoLogger()
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
//        val cv = TraceClassVisitor(DefaultClassVisitor(), PrintWriter(System.out))
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
        val ca = CheckClassAdapter(cw, true)

        val filter = listOf<String>()
        val mcv = MutatedClassVisitor(
            ca, tracker, filter, operators?.filter { it.operatorName == mutationID.operatorName }
        )
        try {
            cr.accept(mcv, ClassReader.EXPAND_FRAMES)
        } catch (e: Exception) {
            logger.error(e.printStackTrace().toString())
        }
        return if (tracker.getTargetMutation() != null) {
            Mutant(tracker.getTargetMutation()!!, cw.toByteArray())
        } else {
            null
        }
    }
}