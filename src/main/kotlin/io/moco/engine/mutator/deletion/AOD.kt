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
 * AOD - Arithmetic Operator Deletion
 *
 * @property operator
 * @property tracker
 * @property methodInfo
 * @constructor
 *
 * @param delegateMethodVisitor
 */
class AOD(
    val operator: DeletionOperator,
    tracker: MutatedMethodTracker,
    private val methodInfo: MethodInfo,
    delegateMethodVisitor: MethodVisitor
) : DeletionMutator(tracker, methodInfo, delegateMethodVisitor) {

    override val opcodeDesc: Map<Int, Pair<String, String>> = mapOf(
        Opcodes.IADD to Pair("integer addition", "IADD"), Opcodes.ISUB to Pair("integer subtraction", "ISUB"),
        Opcodes.IMUL to Pair("integer multiplication", "IMUL"), Opcodes.IDIV to Pair("integer division", "IDIV"),
        Opcodes.IREM to Pair("integer modulo", "IREM"),

        Opcodes.FADD to Pair("float addition", "FADD"), Opcodes.FSUB to Pair("float subtraction", "FSUB"),
        Opcodes.FMUL to Pair("float multiplication", "FMUL"), Opcodes.FDIV to Pair("float division", "FDIV"),
        Opcodes.FREM to Pair("float modulo", "FREM"),

        Opcodes.LADD to Pair("long addition", "LADD"), Opcodes.LSUB to Pair("long subtraction", "LSUB"),
        Opcodes.LMUL to Pair("long multiplication", "LMUL"), Opcodes.LDIV to Pair("long division", "LDIV"),
        Opcodes.LREM to Pair("long modulo", "LREM"),

        Opcodes.DADD to Pair("double addition", "DADD"), Opcodes.DSUB to Pair("double subtraction", "DSUB"),
        Opcodes.DMUL to Pair("double multiplication", "DMUL"), Opcodes.DDIV to Pair("double division", "DDIV"),
        Opcodes.DREM to Pair("double modulo", "DREM"),
    )

    override val supportedOpcodes = mapOf(
        "int" to listOf(Opcodes.IADD, Opcodes.ISUB, Opcodes.IMUL, Opcodes.IDIV, Opcodes.IREM),
        "float" to listOf(Opcodes.FADD, Opcodes.FSUB, Opcodes.FMUL, Opcodes.FDIV, Opcodes.FREM),
        "long" to listOf(Opcodes.LADD, Opcodes.LSUB, Opcodes.LMUL, Opcodes.LDIV, Opcodes.LREM),
        "double" to listOf(Opcodes.DADD, Opcodes.DSUB, Opcodes.DMUL, Opcodes.DDIV, Opcodes.DREM)
    )

    private val varStoreOpcodes: List<Int> = listOf(Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE)

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        if (varStoreOpcodes.contains(opcode)) {
            val collectedMu = tracker.mutatedClassTracker.getCollectedMutations().filter {
                it.lineOfCode == tracker.currConsideredLineNumber }
            collectedMu.map { it.relatedVarIndices.add(`var`) }
        }
        super.visitVarInsn(opcode, `var`)
    }

    private fun trySecondOperandRemoval(opcode: Int, type: String): Boolean {
        // KEEP_F_$opcode means keep first operand
        val newMutation =
            tracker.registerMutation(
                operator,
                createDesc("delete operand after", opcode),
                createUniqueID(opcode, "KEEP-F"),
                opcodeDesc[opcode]?.second
            ) ?: return false
        if (tracker.mutatedClassTracker.targetMutation != null) {
            // In mutant creation phase, visit corresponding instruction to mutate it
            if (tracker.isTargetMutation(newMutation)) {
                tracker.mutatedClassTracker.setGeneratedTargetMutation(newMutation)
                logger.debug("Delete second operand after arithmetic operator: $opcode")
                return when (type) {
                    "int", "float" -> {
                        mv.visitInsn(Opcodes.POP)
                        true
                    }
                    "long", "double" -> {
                        mv.visitInsn(Opcodes.POP2)
                        true
                    }
                    else -> false
                }
            }
        }
        return false
    }

    private fun tryFirstOperandRemoval(opcode: Int, type: String): Boolean {
        // KEEP_S_$opcode means keep second operand
        val newMutation =
            tracker.registerMutation(
                operator,
                createDesc("delete operand after", opcode),
                createUniqueID(opcode, "KEEP-S"),
                opcodeDesc[opcode]?.second
            ) ?: return false
        if (tracker.mutatedClassTracker.targetMutation != null) {
            // In mutant creation phase, visit corresponding instruction to mutate it
            if (tracker.isTargetMutation(newMutation)) {
                tracker.mutatedClassTracker.setGeneratedTargetMutation(newMutation)
                logger.debug("Delete first operand before arithmetic operator: $opcode")

                when (type) {
                    "int" -> {
                        val temp = newLocal(Type.INT_TYPE)
                        mv.visitVarInsn(Opcodes.ISTORE, temp)
                        mv.visitInsn(Opcodes.POP)
                        mv.visitVarInsn(Opcodes.ILOAD, temp)
                        return true
                    }
                    "float" -> {
                        val temp = newLocal(Type.FLOAT_TYPE)
                        mv.visitVarInsn(Opcodes.FSTORE, temp)
                        mv.visitInsn(Opcodes.POP)
                        mv.visitVarInsn(Opcodes.FLOAD, temp)
                        return true
                    }
                    "long" -> {
                        val temp = newLocal(Type.LONG_TYPE)
                        mv.visitVarInsn(Opcodes.LSTORE, temp)
                        mv.visitInsn(Opcodes.POP2)
                        mv.visitVarInsn(Opcodes.LLOAD, temp)
                        return true
                    }
                    "double" -> {
                        val temp = newLocal(Type.DOUBLE_TYPE)
                        mv.visitVarInsn(Opcodes.DSTORE, temp)
                        mv.visitInsn(Opcodes.POP2)
                        mv.visitVarInsn(Opcodes.DLOAD, temp)

                        return true
                    }
                    else -> return false
                }
            }
        }
        return false
    }

    override fun visitInsn(opcode: Int) {
        var supported: Boolean = false
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
            visited = tryFirstOperandRemoval(opcode, type)
            if (!visited) visited = trySecondOperandRemoval(opcode, type)
            if (!visited) mv.visitInsn(opcode)
        } else mv.visitInsn(opcode)
    }
}