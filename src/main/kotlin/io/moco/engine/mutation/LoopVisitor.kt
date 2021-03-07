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

import io.moco.engine.tracker.MutatedMethodTracker
import io.moco.utils.JavaInfo
import io.moco.utils.MoCoLogger
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


class LoopVisitor(
    delegateMethodVisitor: MethodVisitor?,
    private val mutatedMethodTracker: MutatedMethodTracker,
) : MethodVisitor(JavaInfo.ASM_VERSION, delegateMethodVisitor) {
    private val logger = MoCoLogger()
    private var jumpTracker: MutableList<Pair<Label, Int>>? = mutableListOf()
    private var localVarTracker: MutableMap<Int, MutableSet<Int>>? = mutableMapOf()
    private var mapLineLabel: MutableMap<Label, Int> = mutableMapOf()
    private var lineTracker: MutableList<Int> = mutableListOf()

    override fun visitLabel(label: Label?) {
        if (label != null) {
            mapLineLabel[label] = mutatedMethodTracker.currConsideredLineNumber
        }
        super.visitLabel(label)
    }

    override fun visitLineNumber(line: Int, start: Label?) {
        lineTracker.add(line)
        super.visitLineNumber(line, start)
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        // handle do while
        if (mapLineLabel.keys.contains(label) && opcode != Opcodes.GOTO && opcode != Opcodes.JSR &&
            mutatedMethodTracker.currConsideredLineNumber > mapLineLabel[label]!!
        ) {
            mutatedMethodTracker.doWhileloopTracker.putIfAbsent(
                // Key is a pair of start and end lines of code of the loop
                Pair(mapLineLabel[label]!!, mutatedMethodTracker.currConsideredLineNumber),
                // The value of this map is a list of indices of local variables used for the loop
                localVarTracker?.get(mutatedMethodTracker.currConsideredLineNumber)!!.toList()
            )
        }

        // handle for, while
        if (opcode != Opcodes.GOTO) {
            jumpTracker?.add(
                Pair(
                    mutatedMethodTracker.currConsideredLineLabel!!,
                    mutatedMethodTracker.currConsideredLineNumber
                )
            )
        } else {
            if (!jumpTracker.isNullOrEmpty()) {
                for (item in jumpTracker!!) {
                    if (mutatedMethodTracker.currConsideredLineNumber == item.second) {
                        if (label == item.first) {
                            // loop detected
                            mutatedMethodTracker.forWhileloopTracker.putIfAbsent(
                                // Key is a pair of start and end lines of code of the loop
                                Pair(item.second, lineTracker.elementAt(lineTracker.size - 2)),
                                // The value of this map is a list of indices of local variables used for the loop
                                localVarTracker?.get(item.second)!!.toList()
                            )
                            // Remove the target jump point which is the target of this goto instruction
                        }
                    }
                }
            }
        }
        super.visitJumpInsn(opcode, label)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        // Map from line of code to variable indices
        val curLine = mutatedMethodTracker.currConsideredLineNumber
        if (localVarTracker?.keys?.contains(curLine) == true) {
            localVarTracker?.get(curLine)?.add(`var`)
        } else {
            localVarTracker?.set(curLine, mutableSetOf(`var`))
        }
        super.visitVarInsn(opcode, `var`)
    }

    override fun visitEnd() {
        // Filter out infinite loop mutation at the end of a method visit
        val collectedMu = mutatedMethodTracker.mutatedClassTracker.getCollectedMutations()
        collectedMu.removeAll { mutationForLoopCounters(it, mutatedMethodTracker) }
        super.visitEnd()
    }

    private fun mutationForLoopCounters(mutation: Mutation, mt: MutatedMethodTracker): Boolean {
        // instructions at lines of code of for loop such as for, while, do while, are not mutated
        // because such kind of mutations can lead to infinite loop
        if (mutation.optionalInfo?.get("varIndex") != null) {
            if (mt.forWhileloopTracker.any {
                            it.key.first <= mutation.lineOfCode &&
                            mutation.lineOfCode <= it.key.second &&
                            it.value.contains(mutation.optionalInfo?.get("varIndex")) } ||

                mt.doWhileloopTracker.any {
                    it.key.first <= mutation.lineOfCode &&
                            mutation.lineOfCode <= it.key.second &&
                            it.value.contains(mutation.optionalInfo?.get("varIndex")) } ) {

                logger.debug("Skip loop mutation " +
                        "${mutation.mutationID.location.className?.name} -" +
                        "${mutation.mutationID.location.methodName.name} -" +
                        "${mutation.mutationID.mutatorID}-" +
                        "${mutation.mutationID.instructionIndices}-" +
                        "${mutation.lineOfCode}")
                return true
            }
        }
        return false

//        if (operator.operatorName == "ROR") {
//            // We don't mutate relational operator at line of code for loop (for, while, do while)
//            if (forWhileloopTracker.any { it.key.first == currConsideredLineNumber }) {
//                return true
//            }
//        }
//        return false
    }
}