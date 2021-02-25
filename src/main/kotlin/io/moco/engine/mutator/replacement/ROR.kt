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
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


class ROR(
    operator: ReplacementOperator, tracker: MutatedMethodTracker, delegateMethodVisitor: MethodVisitor
) : ReplacementMutator(operator, tracker, delegateMethodVisitor) {

    override val opcodeDesc: Map<Int, String> = mapOf(
        Opcodes.IFLT to "less than operator",
        Opcodes.IFLE to "less than or equal operator",
        Opcodes.IFGT to "greater than operator",
        Opcodes.IFGE to "greater than or equal operator",
        Opcodes.IFEQ to "equal operator",
        Opcodes.IFNE to "not equal operator",

        Opcodes.IF_ICMPLT to "less than operator",
        Opcodes.IF_ICMPLE to "less than or equal operator",
        Opcodes.IF_ICMPGT to "greater than operator",
        Opcodes.IF_ICMPGE to "greater than or equal operator",
        Opcodes.IF_ICMPEQ to "equal operator",
        Opcodes.IF_ICMPNE to "not equal operator",

        Opcodes.IFNULL to "equal operator",
        Opcodes.IFNONNULL to "not equal operator",

        Opcodes.IF_ACMPEQ to "equal operator",
        Opcodes.IF_ACMPNE to "not equal operator",
        )

    override val supportedOpcodes = mapOf(
        "zero" to listOf(Opcodes.IFLT, Opcodes.IFLE, Opcodes.IFNE, Opcodes.IFEQ, Opcodes.IFGT, Opcodes.IFGE),
        "val" to listOf(
            Opcodes.IF_ICMPLT, Opcodes.IF_ICMPLE, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPEQ,
            Opcodes.IF_ICMPGT, Opcodes.IF_ICMPGE
        ),
        "reference" to listOf(Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE),
        "null" to listOf(Opcodes.IFNULL, Opcodes.IFNONNULL)
    )


    override fun visitJumpInsn(opcode: Int, label: Label?) {
        var supported : Boolean = false
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
                            mv.visitJumpInsn(newOpcode, label)
                            visited = true
                            break
                        }
                    }
                }
            }
            if (!visited) {
                // Go on without mutating bytecode after collecting all possible mutations
                mv.visitJumpInsn(opcode, label)
            }
        } else {
            mv.visitJumpInsn(opcode, label)
        }
    }
}
