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

package io.github.phantran.engine.mutation

import io.github.phantran.engine.tracker.MutatedMethodTracker
import io.github.phantran.utils.JavaInfo
import io.github.phantran.utils.MoCoLogger
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


/**
 * Filter visitor
 *
 * This class is reponsible for filtering collected mutations at the end of the mutations collecting step
 *
 * Currently, most of the implementation of this class is to filter out mutations of loop counters since
 * such kind of mutations usually result in timeout test errors
 *
 * If you wanted to add more types of mutation filter, you could put them in the visitEnd method of this class
 *
 * @property mutatedMethodTracker
 * @constructor
 *
 * @param delegateMethodVisitor
 */
class FilterVisitor(
    delegateMethodVisitor: MethodVisitor?,
    private val mutatedMethodTracker: MutatedMethodTracker,
) : MethodVisitor(JavaInfo.ASM_VERSION, delegateMethodVisitor) {
    private val logger = MoCoLogger()

    // Jump tracker is a list of label to line number pair of jump instruction in a method
    private var jumpTracker: MutableSet<Pair<Label, Int>>? = mutableSetOf()

    // localVarTracker record mapping from line number to variable indices used on that line
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
            if (mutatedMethodTracker.currConsideredLineNumber >= mapLineLabel[label]!! + 1) {
                mutatedMethodTracker.doWhileloopTracker.putIfAbsent(
                    // Key is a pair of start and end lines of code of the loop
                    Pair(mapLineLabel[label]!! + 1, mutatedMethodTracker.currConsideredLineNumber),
                    // The value of this map is a list of indices of local variables used for the loop
                    localVarTracker?.get(mutatedMethodTracker.currConsideredLineNumber)!!.toList()
                )
            }
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
                // when instruction is goto and have previous recorded another type of jump ins
                for (item in jumpTracker!!) {
                    if (mutatedMethodTracker.currConsideredLineNumber >= item.second) {
                        if (label == item.first) {
                            // loop detected
                            var endLine = lineTracker.elementAt(lineTracker.size - 1);
                            if (endLine < item.second) {
                                endLine = lineTracker.elementAt(lineTracker.size - 2);
                            }
                            mutatedMethodTracker.forWhileloopTracker.putIfAbsent(
                                // Key is a pair of start and end lines of code of the loop
                                Pair(item.second, endLine),
                                // The value of this map is a list of indices of local variables used for the loop
                                localVarTracker?.get(item.second)!!.toList()
                            )
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
        // Filter infinite loop mutations
        collectedMu.removeAll { mutationForLoopCounters(it, mutatedMethodTracker) }
        super.visitEnd()
    }

    private fun inLoopCheck(it: Map.Entry<Pair<Int, Int>, List<Int>>, mutation: Mutation): Boolean {
        return (it.key.first <= mutation.lineOfCode &&
                mutation.lineOfCode <= it.key.second &&
                mutation.relatedVarIndices.any { it1 -> it.value.contains(it1) })
    }

    private fun mutationForLoopCounters(mutation: Mutation, mt: MutatedMethodTracker): Boolean {
        // instructions at lines of code of for loop such as for, while, do while, are not mutated
        // because such kind of mutations can lead to infinite loop
        var found = false
        if (mutation.relatedVarIndices.isNotEmpty()) {
            if (mt.forWhileloopTracker.any { inLoopCheck(it, mutation) } ||
                mt.doWhileloopTracker.any { inLoopCheck(it, mutation) }) {
                found = true
            }
        }
        if (mutation.mutationID.operatorName == "ROR") {
            // We don't mutate relational operator on loop line
            if (mt.forWhileloopTracker.any { it.key.first == mutation.lineOfCode } ||
                mt.doWhileloopTracker.any { it.key.second == mutation.lineOfCode }) {
                found = true
            }
        }
        if (found) {
            logger.debug(
                "Skip mutation on loop counters " +
                        "${mutation.mutationID.location.className?.name} -" +
                        "${mutation.mutationID.location.methodName.name} -" +
                        "${mutation.mutationID.mutatorID}-" +
                        "${mutation.mutationID.instructionIndices}-" +
                        "${mutation.lineOfCode}"
            )
            return true
        }
        return false
    }
}