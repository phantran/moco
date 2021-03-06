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

package io.github.phantran.engine.mutator.deletion

import io.github.phantran.engine.MethodInfo
import io.github.phantran.engine.operator.DeletionOperator
import io.github.phantran.engine.tracker.MutatedMethodTracker
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
    tracker: MutatedMethodTracker,
    private val methodInfo: MethodInfo,
    delegateMethodVisitor: MethodVisitor
) : DeletionMutator(tracker, methodInfo, delegateMethodVisitor) {

    override val opcodeDesc: Map<Int, Pair<String, String>> = mapOf(
        Opcodes.IAND to Pair("integer AND", "IAND"), Opcodes.IOR to Pair("integer OR", "IOR"),
        Opcodes.LAND to Pair("long AND", "LAND"), Opcodes.LOR to Pair("long OR", "LOR"),
    )

    override val supportedOpcodes = mapOf(
        "int" to listOf(Opcodes.IAND, Opcodes.IOR),
        "long" to listOf(Opcodes.LAND, Opcodes.LOR),
    )

    private fun trySecondOperandRemoval(opcode: Int, type: String): Boolean {
        // $opcode-KEEP-F means keep first operand
        val newMutation =
            tracker.registerMutation(
                operator,
                createDesc("delete second operand after logical operator", opcode),
                createUniqueID(opcode, "S"),
                opcodeDesc[opcode]?.second
            ) ?: return false
        if (tracker.mutatedClassTracker.targetMutation != null) {
            // In mutant creation phase, visit corresponding instruction to mutate it
            if (tracker.isTargetMutation(newMutation)) {
                tracker.mutatedClassTracker.setGeneratedTargetMutation(newMutation)
                logger.debug("Delete second operand after logical operator: $opcode")
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
        // $opcode-KEEP-S means keep second operand
        val newMutation =
            tracker.registerMutation(
                operator,
                createDesc("delete first operand before logical operator", opcode),
                createUniqueID(opcode, "F"),
                opcodeDesc[opcode]?.second
            ) ?: return false
        if (tracker.mutatedClassTracker.targetMutation != null) {
            // In mutant creation phase, visit corresponding instruction to mutate it
            if (tracker.isTargetMutation(newMutation)) {
                tracker.mutatedClassTracker.setGeneratedTargetMutation(newMutation)
                logger.debug("Delete first operand before logical operator: $opcode")
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
            if (!visited) visited = tryFirstOperandRemoval(opcode, type)
            if (!visited) mv.visitInsn(opcode)
        } else mv.visitInsn(opcode)
    }
}