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

    override val opcodeDesc: Map<Int, String> = mapOf(
        Opcodes.IADD to "integer addition", Opcodes.ISUB to "integer subtraction",
        Opcodes.IMUL to "integer multiplication", Opcodes.IDIV to "integer division",
        Opcodes.IREM to "integer modulo",

        Opcodes.LADD to "long addition", Opcodes.LSUB to "long subtraction",
        Opcodes.LMUL to "long multiplication", Opcodes.LDIV to "long division",
        Opcodes.LREM to "long modulo",

        Opcodes.FADD to "float addition", Opcodes.FSUB to "float subtraction",
        Opcodes.FMUL to "float multiplication", Opcodes.FDIV to "float division",
        Opcodes.FREM to "float modulo",

        Opcodes.DADD to "double addition", Opcodes.DSUB to "double subtraction",
        Opcodes.DMUL to "double multiplication", Opcodes.DDIV to "double division",
        Opcodes.DREM to "double modulo",
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
                    val newMutation = tracker.registerMutation(operator, createDesc(opcode, newOpcode)) ?: continue
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
