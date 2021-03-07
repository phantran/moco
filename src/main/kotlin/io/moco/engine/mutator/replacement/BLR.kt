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

package io.moco.engine.mutator.replacement

import io.moco.engine.operator.ReplacementOperator
import io.moco.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


/**
 * BLR - Bitwise Logical Operator Replacement
 *
 * @constructor
 *
 * @param operator
 * @param tracker
 * @param delegateMethodVisitor
 */
class BLR(
    operator: ReplacementOperator,
    tracker: MutatedMethodTracker,
    delegateMethodVisitor: MethodVisitor
) : ReplacementMutator(operator, tracker, delegateMethodVisitor) {

    override val opcodeDesc: Map<Int, Pair<String, String>> = mapOf(
        Opcodes.IAND to Pair("integer AND", "IAND"), Opcodes.IOR to Pair("integer OR", "IOR"),
        Opcodes.LAND to Pair("long AND", "LAND"), Opcodes.LOR to Pair("long OR", "LOR"),
    )

    override val supportedOpcodes = mapOf(
        "int" to listOf(Opcodes.IAND, Opcodes.IOR),
        "long" to listOf(Opcodes.LAND, Opcodes.LOR),
    )

    private fun operatorReplace(opcode: Int, newOpcode: Int): Boolean {
        // Replace IAND by IOR, or vice versa
        val newMutation = tracker.registerMutation(
            operator, createDesc(opcode, newOpcode),
            createUniqueID(opcode, newOpcode), opcodeDesc[opcode]?.second
        ) ?: return false
        //Collect mutation information
        if (tracker.mutatedClassTracker.targetMutation != null) {
            // In mutant creation phase, visit corresponding instruction to mutate it
            if (tracker.isTargetMutation(newMutation)) {
                tracker.mutatedClassTracker.setGeneratedTargetMutation(newMutation)
                logger.debug("Old Opcode: $opcode")
                logger.debug("New Opcode: $newOpcode")
                mv.visitInsn(newOpcode)
                return true
            }
        }
        return false
    }

    override fun visitInsn(opcode: Int) {
        var supported = false
        var type: String = ""
        for (key in supportedOpcodes.keys) {
            if (supportedOpcodes[key]!!.contains(opcode)) {
                supported = true
                type = key
                break
            }
        }
        var visited = false
        if (supported) {
            for (newOpcode in supportedOpcodes[type]!!) {
                if (newOpcode != opcode) {
                    visited = operatorReplace(opcode, newOpcode)
                    if (visited) break
                }
            }
            if (!visited) {
                // Go on without mutating bytecode after collecting all possible mutations
                mv.visitInsn(opcode)
            }
        } else {
            mv.visitInsn(opcode)
        }
    }
}
