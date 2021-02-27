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

package io.moco.engine.mutator.insertion

import io.moco.engine.operator.InsertionOperator
import io.moco.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


/**
 * POUOI - Post Unary Operator Insertion
 *
 * @constructor
 *
 * @param operator
 * @param tracker
 * @param delegateMethodVisitor
 */
class POUOI(
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
        Pair("post-increment", "POI"), Pair("post-decrement", "POD"),
    )

    override fun visitVarInsn(opcode: Int, v: Int) {
        var supported = false
        if (supportedOpcodes["var"]!!.contains(opcode) &&
            (!tracker.mutatedClassTracker.getClsInfo()?.isEnum!!)) {
            supported = true
        }
        mv.visitVarInsn(opcode, v)
        if (supported) {
            for (operatorType in operatorTypes) {
                // Always collect mutation information in both collecting and creating phase
                val newMutation = tracker.registerMutation(
                    operator,
                    createDesc(operatorType.first, opcode), createUniqueID(operatorType.second, opcode)
                ) ?: continue
                // But only do visiting to create actual mutant if in creating phase
                if (tracker.mutatedClassTracker.targetMutationID != null) {
                    if (tracker.isTargetMutation(newMutation.mutationID)) {
                        tracker.mutatedClassTracker.setTargetMutation(newMutation)
                        logger.debug("${operatorType.first} of variable : $v")
                        when (operatorType.second) {
                            "POI" -> handleVarPostOp(opcode, v)
                            "POD" -> handleVarPostOp(opcode, v, false)
                        }
                        break
                    }

                }
            }
        }
    }

    private fun handleVarPostOp(opcode: Int, v: Int, isInc: Boolean = true) {
        when (opcode) {
            Opcodes.ILOAD -> mv.visitIincInsn(v, if (isInc) 1 else -1)
            Opcodes.FLOAD -> {
                mv.visitInsn(Opcodes.DUP)
                mv.visitInsn(Opcodes.FCONST_1)
                mv.visitInsn(if (isInc) Opcodes.FADD else Opcodes.FSUB)
                mv.visitVarInsn(Opcodes.FSTORE, v)
            }
            Opcodes.LLOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(Opcodes.LCONST_1)
                mv.visitInsn(if (isInc) Opcodes.LADD else Opcodes.LSUB)
                mv.visitVarInsn(Opcodes.LSTORE, v)
            }
            Opcodes.DLOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(Opcodes.DCONST_1)
                mv.visitInsn(if (isInc) Opcodes.DADD else Opcodes.DSUB)
                mv.visitVarInsn(Opcodes.DSTORE, v)
            }
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
                    createDesc(operatorType.first, opcode), createUniqueID(operatorType.second, opcode)
                ) ?: continue
                // But only do visiting to create actual mutant if still in creating phase
                if (tracker.mutatedClassTracker.targetMutationID != null) {
                    if (tracker.isTargetMutation(newMutation.mutationID)) {
                        tracker.mutatedClassTracker.setTargetMutation(newMutation)
                        logger.debug("${operatorType.first} of array element")
                        when (operatorType.second) {
                            "POI" -> visited = handleArrPostOp(opcode)
                            "POD" -> visited = handleArrPostOp(opcode, false)
                        }
                        if (visited) break
                    }
                }
            }
            if (!visited) {
                mv.visitInsn(opcode)
            }
        } else {
            mv.visitInsn(opcode)
        }
    }

    private fun handleArrPostOp(opcode: Int, isInc: Boolean = true): Boolean {
        when (opcode) {
            Opcodes.IALOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(opcode)
                mv.visitInsn(Opcodes.DUP_X2)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitInsn(if (isInc) Opcodes.IADD else Opcodes.ISUB)
                mv.visitInsn(Opcodes.IASTORE)
                return true
            }
            Opcodes.FALOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(opcode)
                mv.visitInsn(Opcodes.DUP_X2)
                mv.visitInsn(Opcodes.FCONST_1)
                mv.visitInsn(if (isInc) Opcodes.FADD else Opcodes.FSUB)
                mv.visitInsn(Opcodes.FASTORE)
                return true
            }
            Opcodes.LALOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(opcode)
                mv.visitInsn(Opcodes.DUP2_X2)
                mv.visitInsn(Opcodes.LCONST_1)
                mv.visitInsn(if (isInc) Opcodes.LADD else Opcodes.LSUB)
                mv.visitInsn(Opcodes.LASTORE)
                return true
            }
            Opcodes.DALOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(opcode)
                mv.visitInsn(Opcodes.DUP2_X2)
                mv.visitInsn(Opcodes.DCONST_1)
                mv.visitInsn(if (isInc) Opcodes.DADD else Opcodes.DSUB)
                mv.visitInsn(Opcodes.DASTORE)
                return true
            }
            Opcodes.BALOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(opcode)
                mv.visitInsn(Opcodes.DUP_X2)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitInsn(if (isInc) Opcodes.IADD else Opcodes.ISUB)
                mv.visitInsn(Opcodes.I2B)
                mv.visitInsn(Opcodes.BASTORE)
                return true
            }
            Opcodes.SALOAD -> {
                mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(opcode)
                mv.visitInsn(Opcodes.DUP_X2)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitInsn(if (isInc) Opcodes.IADD else Opcodes.ISUB)
                mv.visitInsn(Opcodes.I2S)
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
                    createDesc(operatorType.first, opcode), createUniqueID("${operatorType.second}_$desc", opcode)
                ) ?: continue

                // But only do visiting to create actual mutant if still in creating phase
                if (tracker.mutatedClassTracker.targetMutationID != null) {

                    if (tracker.isTargetMutation(newMutation.mutationID)) {
                        tracker.mutatedClassTracker.setTargetMutation(newMutation)
                        logger.debug("${operatorType.first} of ${opcodeDesc[opcode]?.first}")

                        when (operatorType.second) {
                            "POI" -> visited = handleFieldPostOp(opcode, owner, name, desc)
                            "POD" -> visited = handleFieldPostOp(opcode, owner, name, desc, false)
                        }
                    }
                }
                if (visited) break
            }
            if (!visited) {
                mv.visitFieldInsn(opcode, owner, name, desc)
            }
        } else {
            mv.visitFieldInsn(opcode, owner, name, desc)
        }
    }

    private fun handleFieldPostOp(
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
                if (isNormalField) mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(opcode, owner, name, desc)
                if (isNormalField) mv.visitInsn(Opcodes.DUP_X1) else mv.visitInsn(Opcodes.DUP)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitInsn(if (isInc) Opcodes.IADD else Opcodes.ISUB)
                // PUTFIELD if normal object field, PUTSTATIC if static field
                mv.visitFieldInsn(if (isNormalField) Opcodes.PUTFIELD else Opcodes.PUTSTATIC, owner, name, desc)
                return true
            }
            "F" -> {
                if (isNormalField) mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(opcode, owner, name, desc)
                if (isNormalField) mv.visitInsn(Opcodes.DUP_X1) else mv.visitInsn(Opcodes.DUP)
                mv.visitInsn(Opcodes.FCONST_1)
                mv.visitInsn(if (isInc) Opcodes.FADD else Opcodes.FSUB)
                mv.visitFieldInsn(if (isNormalField) Opcodes.PUTFIELD else Opcodes.PUTSTATIC, owner, name, desc)
                return true
            }
            "J" -> {
                if (isNormalField) mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(opcode, owner, name, desc)
                if (isNormalField) mv.visitInsn(Opcodes.DUP2_X1) else mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(Opcodes.LCONST_1)
                mv.visitInsn(if (isInc) Opcodes.LADD else Opcodes.LSUB)
                mv.visitFieldInsn(if (isNormalField) Opcodes.PUTFIELD else Opcodes.PUTSTATIC, owner, name, desc)
                return true
            }
            "D" -> {
                if (isNormalField) mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(opcode, owner, name, desc)
                if (isNormalField) mv.visitInsn(Opcodes.DUP2_X1) else mv.visitInsn(Opcodes.DUP2)
                mv.visitInsn(Opcodes.DCONST_1)
                mv.visitInsn(if (isInc) Opcodes.DADD else Opcodes.DSUB)
                mv.visitFieldInsn(if (isNormalField) Opcodes.PUTFIELD else Opcodes.PUTSTATIC, owner, name, desc)
                return true
            }
            "B" -> {
                if (isNormalField) mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(opcode, owner, name, desc)
                if (isNormalField) mv.visitInsn(Opcodes.DUP_X1) else mv.visitInsn(Opcodes.DUP)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitInsn(if (isInc) Opcodes.IADD else Opcodes.ISUB)
                mv.visitInsn(Opcodes.I2B)
                mv.visitFieldInsn(if (isNormalField) Opcodes.PUTFIELD else Opcodes.PUTSTATIC, owner, name, desc)
                return true
            }
            "S" -> {
                if (isNormalField) mv.visitInsn(Opcodes.DUP)
                mv.visitFieldInsn(opcode, owner, name, desc)
                if (isNormalField) mv.visitInsn(Opcodes.DUP_X1) else mv.visitInsn(Opcodes.DUP)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitInsn(if (isInc) Opcodes.IADD else Opcodes.ISUB)
                mv.visitInsn(Opcodes.I2S)
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