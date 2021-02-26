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


class AOR(
    operator: ReplacementOperator,
    tracker: MutatedMethodTracker,
    delegateMethodVisitor: MethodVisitor
) : ReplacementMutator(operator, tracker, delegateMethodVisitor) {

    override val opcodeDesc: Map<Int, Pair<String, String>> = mapOf(
        Opcodes.IADD to Pair("integer addition", "IADD"), Opcodes.ISUB to Pair("integer subtraction","ISUB"),
        Opcodes.IMUL to Pair("integer multiplication", "IMUL"), Opcodes.IDIV to Pair("integer division","IDIV"),
        Opcodes.IREM to Pair("integer modulo", "IREM"),

        Opcodes.LADD to Pair("long addition", "LADD"), Opcodes.LSUB to Pair("long subtraction","LSUB"),
        Opcodes.LMUL to Pair("long multiplication", "LMUL"), Opcodes.LDIV to Pair("long division","LDIV"),
        Opcodes.LREM to Pair("long modulo", "LREM"),

        Opcodes.FADD to Pair("float addition", "FADD"), Opcodes.FSUB to Pair("float subtraction","FSUB"),
        Opcodes.FMUL to Pair("float multiplication", "FMUL"), Opcodes.FDIV to Pair("float division","FDIV"),
        Opcodes.FREM to Pair("float modulo", "FREM"),

        Opcodes.DADD to Pair("double addition", "DADD"), Opcodes.DSUB to Pair("double subtraction","DSUB"),
        Opcodes.DMUL to Pair("double multiplication", "DMUL"), Opcodes.DDIV to Pair("double division", "DDIV"),
        Opcodes.DREM to Pair("double modulo", "DREM"),
    )

    override val supportedOpcodes = mapOf(
        "int" to listOf(Opcodes.IADD, Opcodes.ISUB, Opcodes.IMUL, Opcodes.IDIV, Opcodes.IREM),
        "long" to listOf(Opcodes.LADD, Opcodes.LSUB, Opcodes.LMUL, Opcodes.LDIV, Opcodes.LREM),
        "float" to listOf(Opcodes.FADD, Opcodes.FSUB, Opcodes.FMUL, Opcodes.FDIV, Opcodes.FREM),
        "double" to listOf(Opcodes.DADD, Opcodes.DSUB, Opcodes.DMUL, Opcodes.DDIV, Opcodes.DREM)
    )

    override fun visitInsn(opcode: Int) {
        var supported = false
        var type = ""
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
                    // Collect mutation information
                    val newMutation = tracker.registerMutation(operator,
                        createDesc(opcode, newOpcode), createUniqueID(opcode, newOpcode)) ?: continue
                    if (tracker.mutatedClassTracker.targetMutationID != null) {
                        // In mutant creation phase, visit corresponding instruction to mutate it
                        if (tracker.isTargetMutation(newMutation.mutationID)) {
                            tracker.mutatedClassTracker.setTargetMutation(newMutation)
                            logger.debug("Old Opcode: $opcode")
                            logger.debug("New Opcode: $newOpcode")
                            mv.visitInsn(newOpcode)
                            visited = true
                            break
                        }
                    }
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
