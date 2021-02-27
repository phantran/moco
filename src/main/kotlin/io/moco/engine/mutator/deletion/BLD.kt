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

package io.moco.engine.mutator.deletion

import io.moco.engine.MethodInfo
import io.moco.engine.operator.DeletionOperator
import io.moco.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

/**
 * BLD - Bitwise Logical Operator Deletion
 *
 * @property operator
 * @property tracker
 * @property methodInfo
 * @constructor
 *
 * @param delegateMethodVisitor
 */
class BLD(
    val operator: DeletionOperator,
    val tracker: MutatedMethodTracker,
    private val methodInfo: MethodInfo,
    delegateMethodVisitor: MethodVisitor
) : DeletionMutator(methodInfo, delegateMethodVisitor) {

    override val opcodeDesc: Map<Int, Pair<String, String>> = mapOf(
        Opcodes.IAND to Pair("integer AND", "IAND"), Opcodes.IOR to Pair("integer OR", "IOR"),
        Opcodes.LAND to Pair("long AND", "LAND"), Opcodes.LOR to Pair("long OR", "LOR"),
    )

    override val supportedOpcodes = mapOf(
        "int" to listOf(Opcodes.IAND, Opcodes.IOR),
        "long" to listOf(Opcodes.LAND, Opcodes.LOR),
    )

    private fun trySecondOperandRemoval(opcode: Int, type: String): Boolean {
        // KEEP_F_$opcode means keep first operand
        val newMutation =
            tracker.registerMutation(
                operator,
                "removal of second operand after logical operator " +
                        "${opcodeDesc[opcode]?.second?.substring(1)}",
                "KEEP_F_${opcodeDesc[opcode]?.second}"
            ) ?: return false
        if (tracker.mutatedClassTracker.targetMutationID != null) {
            // In mutant creation phase, visit corresponding instruction to mutate it
            if (tracker.isTargetMutation(newMutation.mutationID)) {
                tracker.mutatedClassTracker.setTargetMutation(newMutation)
                logger.debug("Remove second operand after logical operator: $opcode")
                when (type) {
                    "long" -> mv.visitInsn(Opcodes.POP2)
                    "int" -> mv.visitInsn(Opcodes.POP)
                    else -> return false
                }
                return true
            }
        }
        return false
    }

    private fun tryFirstOperandRemoval(opcode: Int, type: String): Boolean {
        // KEEP_S_$opcode means keep second operand
        val newMutation =
            tracker.registerMutation(
                operator,
                "removal of first operand before logical operator " +
                        "${opcodeDesc[opcode]?.second?.substring(1)}",
                "KEEP_S_${opcodeDesc[opcode]?.second}"
            ) ?: return false
        if (tracker.mutatedClassTracker.targetMutationID != null) {
            // In mutant creation phase, visit corresponding instruction to mutate it
            if (tracker.isTargetMutation(newMutation.mutationID)) {
                tracker.mutatedClassTracker.setTargetMutation(newMutation)
                logger.debug("Remove first operand before logical operator: $opcode")
                when (type) {
                    "long" -> {
                        val temp = this.newLocal(Type.LONG_TYPE)
                        mv.visitVarInsn(Opcodes.LSTORE, temp)
                        mv.visitInsn(Opcodes.POP2)
                        mv.visitVarInsn(Opcodes.LLOAD, temp)
                    }
                    "int" -> {
                        val temp = this.newLocal(Type.INT_TYPE)
                        mv.visitVarInsn(Opcodes.ISTORE, temp)
                        mv.visitInsn(Opcodes.POP)
                        mv.visitVarInsn(Opcodes.ILOAD, temp)
                    }
                    else -> return false
                }
                return true
            }
        }
        return false
    }

    override fun visitInsn(opcode: Int) {
        var supported = false
        var type = ""
        for (key in supportedOpcodes.keys) {
            if (supportedOpcodes[key]!!.contains(opcode) && (!methodInfo.isGeneratedEnumMethod)) {
                supported = true
                type = key
                break
            }
        }
        var visited: Boolean
        if (supported) {
            visited = trySecondOperandRemoval(opcode, type)
            if (!visited) {
                visited = tryFirstOperandRemoval(opcode, type)
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