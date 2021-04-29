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

import io.moco.engine.preprocessing.DefaultClassVisitor
import io.moco.utils.ByteArrayLoader
import io.moco.engine.operator.Operator
import io.moco.engine.tracker.MutatedClassTracker
import io.moco.utils.JavaInfo
import io.moco.utils.MoCoLogger
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter
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
     * @param clsNameToMutate
     * @param coveredLines
     * @return Collected possible mutations
     *///TODO: add filter as a property of this class to excluded classes and functions specify by users
    fun findPossibleMutationsOfClass(clsNameToMutate: String,
                                     coveredLines: Set<Int>?,
                                     filterMutants: Boolean = false): List<Mutation> {
        val tracker = MutatedClassTracker(coveredLines = coveredLines, filterMutants=filterMutants)
        val bytesArray: ByteArray? = bytesArrayLoader.getByteArray(clsNameToMutate)
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
        val mcv = MutatedClassVisitor(DefaultClassVisitor(), tracker, filter, operators, true)
        cr.accept(mcv, ClassReader.EXPAND_FRAMES)
        return tracker.getCollectedMutations()
    }

    /**
     * Create actual mutant`
     *
     * @param mutation
     * @param byteArray
     * @return
     */
    fun createMutant(mutation: Mutation, byteArray: ByteArray?): Mutant? {
        // ASM support automatic frame computation since java 7, user COMPUTE_MAXS for version less than 7
        val java7Version = 51
        val cwOption = if (JavaInfo.bytecodeJVersion(byteArray) > java7Version)
            ClassWriter.COMPUTE_FRAMES else ClassWriter.COMPUTE_MAXS
        val tracker = MutatedClassTracker(targetMutation = mutation)
        val cr = ClassReader(byteArray)
        val cw = ClassWriter(cwOption)
//        val cv = TraceClassVisitor(cw, PrintWriter(System.out))
        val ca = CheckClassAdapter(cw)

        val filter = listOf<String>()
        val mcv = MutatedClassVisitor(
            ca, tracker, filter,
            operators?.filter { it.operatorName == mutation.mutationID.operatorName }, false
        )
        try {
            cr.accept(mcv, ClassReader.EXPAND_FRAMES)
        } catch (e: Exception) {
            logger.error(e.printStackTrace().toString())
        }
        return if (tracker.getGeneratedTargetMutation() != null) {
            Mutant(tracker.getGeneratedTargetMutation()!!, cw.toByteArray())
        } else {
            null
        }
    }
}