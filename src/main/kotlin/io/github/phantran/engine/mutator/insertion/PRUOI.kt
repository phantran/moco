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

package io.github.phantran.engine.mutator.insertion

import io.github.phantran.engine.operator.InsertionOperator
import io.github.phantran.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * PRUOI - Pre Unary Operator Insertion
 *
 * @constructor
 *
 * @param operator
 * @param tracker
 * @param delegateMethodVisitor
 */
class PRUOI(
    operator: InsertionOperator,
    tracker: MutatedMethodTracker,
    delegateMethodVisitor: MethodVisitor
) : InsertionMutator(operator, tracker, delegateMethodVisitor) {

    override val opcodeDesc: Map<Int, Pair<String, String>> = mapOf(
        Opcodes.ILOAD to Pair("local variable type integer", "ILOAD"),
        Opcodes.LLOAD to Pair("local variable type long", "LLOAD"),
        Opcodes.FLOAD to Pair("local variable type float", "FLOAD"),
        Opcodes.DLOAD to Pair("local variable type double", "DLOAD"),

        Opcodes.IALOAD to Pair("element of array type integer", "IALOAD"),
        Opcodes.FALOAD to Pair("element of array type float", "FALOAD"),
        Opcodes.LALOAD to Pair("element of array type long", "LALOAD"),
        Opcodes.DALOAD to Pair("element of array type double", "DALOAD"),
        Opcodes.BALOAD to Pair("element of array type byte", "BALOAD"),
        Opcodes.SALOAD to Pair("element of array type short", "SALOAD"),

        Opcodes.GETFIELD to Pair("object field", "GETFIELD"),
        Opcodes.GETSTATIC to Pair("static field", "GETSTATIC"),
    )

    override val supportedOpcodes = mapOf(
        "var" to listOf(Opcodes.ILOAD, Opcodes.FLOAD, Opcodes.LLOAD, Opcodes.DLOAD),
        "arr" to listOf(Opcodes.IALOAD, Opcodes.FALOAD, Opcodes.LALOAD, Opcodes.DALOAD, Opcodes.BALOAD, Opcodes.SALOAD),
        "field" to listOf(Opcodes.GETFIELD, Opcodes.GETSTATIC)
    )

    private val operatorTypes = listOf(
        Pair("pre-increment", "I"), Pair("pre-decrement", "D"),
    )

    override fun visitVarInsn(opcode: Int, v: Int) {
        var supported = false
        if (supportedOpcodes["var"]!!.contains(opcode) && (!tracker.mutatedClassTracker.getClsInfo()?.isEnum!!)) {
            supported = true
        }
        var visited = false
        if (supported) {
            for (operatorType in operatorTypes) {
                // Always collect mutation information in both collecting and creating phase
                val newMutation = tracker.registerMutation(
                    operator,
                    createDesc(operatorType.first, opcode),
                    createUniqueID(opcode, "PR", operatorType.second),
                    opcodeDesc[opcode]?.second, mutableSetOf(v)
                )
                // But only do visiting to create actual mutant if in creating phase
                if (tracker.mutatedClassTracker.targetMutation != null) {
                    if (tracker.isTargetMutation(newMutation)) {
                        tracker.mutatedClassTracker.setGeneratedTargetMutation(newMutation!!)
                        logger.debug("${operatorType.first} of variable : $v")
                        varIndexToLineNo = Pair(v, tracker.currConsideredLineNumber)
                        when (operatorType.second) {
                            "I" -> visited = handleVarPreOp(opcode, v)
                            "D" -> visited = handleVarPreOp(opcode, v, false)
                        }

                    }
                }
            }
            if (!visited) mv.visitVarInsn(opcode, v)
        } else {
            mv.visitVarInsn(opcode, v)
        }
    }

    private fun handleVarPreOp(opcode: Int, v: Int, isInc: Boolean = true): Boolean {
        when (opcode) {
            Opcodes.ILOAD -> {
                mv.visitIincInsn(v, if (isInc) 1 else -1)
                mv.visitVarInsn(opcode, v)
                return true
            }
            Opcodes.FLOAD -> {
                mv.visitVarInsn(opcode, v)
                mv.visitInsn(Opcodes.FCONST_1)
                mv.visitInsn(if (isInc) Opcodes.FADD else Opcodes.FSUB)
                mv.visitVarInsn(Opcodes.FSTORE, v)
                mv.visitVarInsn(opcode, v)
                return true
            }
            Opcodes.LLOAD -> {
                mv.visitVarInsn(opcode, v)
                mv.visitInsn(Opcodes.LCONST_1)
                mv.visitInsn(if (isInc) Opcodes.LADD else Opcodes.LSUB)
                mv.visitVarInsn(Opcodes.LSTORE, v)
                mv.visitVarInsn(opcode, v)
                return true
            }
            Opcodes.DLOAD -> {
                mv.visitVarInsn(opcode, v)
                mv.visitInsn(Opcodes.DCONST_1)
                mv.visitInsn(if (isInc) Opcodes.DADD else Opcodes.DSUB)
                mv.visitVarInsn(Opcodes.DSTORE, v)
                mv.visitVarInsn(opcode, v)
                return true
            }
            else -> return false
        }
    }

    override fun visitInsn(opcode: Int) {
        var supported = false
        if (supportedOpcodes["arr"]!!.contains(opcode) && (!tracker.mutatedClassTracker.getClsInfo()?.isEnum!!)) {
            supported = true
        }
        var visited = false
        if (supported) {
            for (operatorType in operatorTypes) {
                // Always collect mutation information in both collecting and creating phase
                val newMutation = tracker.registerMutation(
                    operator,
                    createDesc(operatorType.first, opcode),
                    createUniqueID(opcode, "PR", operatorType.second),
                    opcodeDesc[opcode]?.second
                )
                // But only do visiting to create actual mutant if still in creating phase
                if (tracker.mutatedClassTracker.targetMutation != null) {
                    if (tracker.isTargetMutation(newMutation)) {
                        tracker.mutatedClassTracker.setGeneratedTargetMutation(newMutation!!)
                        logger.debug("${operatorType.first} of array element")
                        when (operatorType.second) {
                            "I" -> visited = handleArrPreOp(opcode)
                            "D" -> visited = handleArrPreOp(opcode, false)
                        }
                    }
                }
            }
            if (!visited) mv.visitInsn(opcode)
        } else {
            mv.visitInsn(opcode)
        }
    }

    private fun handleArrPreOp(opcode: Int, isInc: Boolean = true): Boolean {
        when (opcode) {
            Opcodes.IALOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(opcode)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitInsn(if (isInc) Opcodes.IADD else Opcodes.ISUB)
                mv.visitInsn(Opcodes.DUP_X2)
                mv.visitInsn(Opcodes.IASTORE)
                return true
            }
            Opcodes.FALOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(opcode)
                mv.visitInsn(Opcodes.FCONST_1)
                mv.visitInsn(if (isInc) Opcodes.FADD else Opcodes.FSUB)
                mv.visitInsn(Opcodes.DUP_X2)
                mv.visitInsn(Opcodes.FASTORE)
                return true
            }
            Opcodes.LALOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(opcode)
                mv.visitInsn(Opcodes.LCONST_1)
                mv.visitInsn(if (isInc) Opcodes.LADD else Opcodes.LSUB)
                mv.visitInsn(Opcodes.DUP2_X2)
                mv.visitInsn(Opcodes.LASTORE)
                return true
            }
            Opcodes.DALOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(opcode)
                mv.visitInsn(Opcodes.DCONST_1)
                mv.visitInsn(if (isInc) Opcodes.DADD else Opcodes.DSUB)
                mv.visitInsn(Opcodes.DUP2_X2)
                mv.visitInsn(Opcodes.DASTORE)
                return true
            }
            Opcodes.BALOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(opcode)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitInsn(if (isInc) Opcodes.IADD else Opcodes.ISUB)
                mv.visitInsn(Opcodes.I2B)
                mv.visitInsn(Opcodes.DUP_X2)
                mv.visitInsn(Opcodes.BASTORE)
                return true
            }
            Opcodes.SALOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(opcode)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitInsn(if (isInc) Opcodes.IADD else Opcodes.ISUB)
                mv.visitInsn(Opcodes.I2S)
                mv.visitInsn(Opcodes.DUP_X2)
                mv.visitInsn(Opcodes.SASTORE)
                return true
            }
            else -> return false
        }
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
        var supported = false
        if (supportedOpcodes["field"]!!.contains(opcode) &&
            (!tracker.mutatedClassTracker.getClsInfo()?.isEnum!!) &&
            listOf("I", "F", "J", "D", "B", "S").contains(desc)
        ) {
            supported = true
        }
        var visited = false
        if (supported) {
            for (operatorType in operatorTypes) {
                // Always collect mutation information in both collecting and creating phase
                val newMutation = tracker.registerMutation(
                    operator,
                    createDesc(operatorType.first, opcode),
                    createUniqueID(opcode, "PR", operatorType.second),
                    opcodeDesc[opcode]?.second
                )
                // But only do visiting to create actual mutant if still in creating phase
                if (tracker.mutatedClassTracker.targetMutation != null) {
                    if (tracker.isTargetMutation(newMutation)) {
                        tracker.mutatedClassTracker.setGeneratedTargetMutation(newMutation!!)
                        logger.debug("${operatorType.first} of ${opcodeDesc[opcode]?.first}")
                        tracker.mutatedClassTracker.targetMutation.additionalInfo["fieldName"] = name
                        when (operatorType.second) {
                            "I" -> visited = handleFieldPreOp(opcode, owner, name, desc)
                            "D" -> visited = handleFieldPreOp(opcode, owner, name, desc, false)
                        }
                    }
                }
            }
            if (!visited)
                mv.visitFieldInsn(opcode, owner, name, desc)
        } else {
            mv.visitFieldInsn(opcode, owner, name, desc)
        }
    }

    private fun handleFieldPreOp(
        opcode: Int,
        owner: String,
        name: String,
        desc: String,
        isInc: Boolean = true
    ): Boolean {
        // Check if accessing normal object field or static field to perform corresponding instruction visit
        val isNormalField = opcode == Opcodes.GETFIELD

        when (desc) {
            "I" -> {
                if (isNormalField) {
                    mv.visitInsn(Opcodes.DUP)
                }
                mv.visitFieldInsn(opcode, owner, name, desc)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitInsn(if (isInc) Opcodes.IADD else Opcodes.ISUB)
                if (isNormalField) mv.visitInsn(Opcodes.DUP_X1) else mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(if (isNormalField) Opcodes.PUTFIELD else Opcodes.PUTSTATIC, owner, name, desc)
                return true
            }
            "F" -> {
                if (isNormalField) mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(opcode, owner, name, desc)
                mv.visitInsn(Opcodes.FCONST_1)
                mv.visitInsn(if (isInc) Opcodes.FADD else Opcodes.FSUB)
                if (isNormalField) mv.visitInsn(Opcodes.DUP_X1) else mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(if (isNormalField) Opcodes.PUTFIELD else Opcodes.PUTSTATIC, owner, name, desc)
                return true
            }
            "J" -> {
                if (isNormalField) mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(opcode, owner, name, desc)
                mv.visitInsn(Opcodes.LCONST_1)
                mv.visitInsn(if (isInc) Opcodes.LADD else Opcodes.LSUB)
                if (isNormalField) mv.visitInsn(Opcodes.DUP2_X1) else mv.visitInsn(Opcodes.DUP2)
                mv.visitFieldInsn(if (isNormalField) Opcodes.PUTFIELD else Opcodes.PUTSTATIC, owner, name, desc)
                return true
            }
            "D" -> {
                if (isNormalField) mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(opcode, owner, name, desc)
                mv.visitInsn(Opcodes.DCONST_1)
                mv.visitInsn(if (isInc) Opcodes.DADD else Opcodes.DSUB)
                if (isNormalField) mv.visitInsn(Opcodes.DUP2_X1) else mv.visitInsn(Opcodes.DUP2)
                mv.visitFieldInsn(if (isNormalField) Opcodes.PUTFIELD else Opcodes.PUTSTATIC, owner, name, desc)
                return true
            }
            "B" -> {
                if (isNormalField) mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(opcode, owner, name, desc)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitInsn(if (isInc) Opcodes.IADD else Opcodes.ISUB)
                mv.visitInsn(Opcodes.I2B)
                if (isNormalField) mv.visitInsn(Opcodes.DUP_X1) else mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(if (isNormalField) Opcodes.PUTFIELD else Opcodes.PUTSTATIC, owner, name, desc)
                return true
            }
            "S" -> {
                if (isNormalField) mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(opcode, owner, name, desc)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitInsn(if (isInc) Opcodes.IADD else Opcodes.ISUB)
                mv.visitInsn(Opcodes.I2S)
                if (isNormalField) mv.visitInsn(Opcodes.DUP_X1) else mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(if (isNormalField) Opcodes.PUTFIELD else Opcodes.PUTSTATIC, owner, name, desc)
                return true
            }
            else -> return false
        }
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        super.visitMaxs(maxStack + 5, maxLocals + 5)
    }
}