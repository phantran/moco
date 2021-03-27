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


package io.moco.engine.tracker

import io.moco.engine.mutation.Mutation
import io.moco.engine.mutation.MutatedMethodLocation
import io.moco.engine.mutation.MutationID
import io.moco.engine.operator.Operator
import io.moco.utils.MoCoLogger
import org.objectweb.asm.Label

/**
 * Mutated method tracker
 *
 * @property mutatedClassTracker
 * @property mutatedMethodLocation
 * @constructor Create empty Mutated method tracker
 */
class MutatedMethodTracker(
    val mutatedClassTracker: MutatedClassTracker,
    private val mutatedMethodLocation: MutatedMethodLocation
) {
    val logger = MoCoLogger()

    var instructionIndex = 0
    var currConsideredLineNumber = 0
    var currConsideredLineLabel: Label? = null

    // List of instruction
    var instructionsOrder: MutableList<String> = mutableListOf()

    // Key of map is pair(start, end), it records start and end lines of code of loop
    // Value of map is list of local variable index that are used for loop counter
    val forWhileloopTracker: MutableMap<Pair<Int, Int>, List<Int>> = mutableMapOf()
    val doWhileloopTracker: MutableMap<Pair<Int, Int>, List<Int>> = mutableMapOf()

    /**
     * Register mutation
     * This method record a possible mutation to mutated class tracker which would be a part of
     * the output of this mutation collecting step
     * @param operator
     * @param description
     * @param mutatorID
     * @return
     */
    fun registerMutation(
        operator: Operator,
        description: String,
        mutatorID: String,
        opcode: String? = "",
        relatedVarIndices: MutableSet<Int> = mutableSetOf()
    ): Mutation? {
        if (operator.notReachedTypeQuantityLimit(mutatedClassTracker.mutationLimitTracker, currConsideredLineNumber)) {
            val newMutationID = MutationID(
                mutatedMethodLocation, mutableListOf(instructionIndex),
                operator.operatorName, mutatorID
            )
            collectInstructionsOrder(opcode, newMutationID)

            // MoCo does not collect mutations on a line that is uncovered by the tests.
            if (!mutatedClassTracker.coveredLines.isNullOrEmpty()) {
                if (!mutatedClassTracker.coveredLines.contains(currConsideredLineNumber)) {
                    return null
                }
            }
            // Only collect unique mutation (mutation id must be unique)
            if (mutatedClassTracker.shouldCollectThisMutation(newMutationID)) {
                return null
            }

            // MoCo does not collect mutations in static init block
            if (mutatedMethodLocation.methodName.name == "<clinit>") {
                return null
            }

            val newMutation = Mutation(
                newMutationID,
                mutatedClassTracker.getFileName(),
                currConsideredLineNumber,
                description,
                relatedVarIndices
            )

            this.mutatedClassTracker.addMutation(newMutation)
            updateLimitByTypeTracker(operator.operatorName)
            return newMutation
        } else return null
    }

    private fun updateLimitByTypeTracker(operatorName: String) {
        if (mutatedClassTracker.mutationLimitTracker?.containsKey(currConsideredLineNumber) == true) {
            mutatedClassTracker.mutationLimitTracker[currConsideredLineNumber]?.add(operatorName)
        } else {
            mutatedClassTracker.mutationLimitTracker?.set(
                currConsideredLineNumber,
                mutableSetOf(operatorName)
            )
        }
    }

    private fun collectInstructionsOrder(opcode: String?, newMutationID: MutationID) {
        // Order of opcodes is collected so we can reconstruct the mutated line of code by using
        // moco.json in a third party tool such as Gamekins
        // same line of code as target mutation
        if (currConsideredLineNumber == mutatedClassTracker.targetMutation?.lineOfCode && !opcode.isNullOrEmpty()) {
            val newInstructionIndices = newMutationID.instructionIndices!!.joinToString(",")
            if (instructionsOrder.isEmpty()) {
                // same opcode
                if (mutatedClassTracker.originalOpcode != null && opcode == mutatedClassTracker.originalOpcode) {
                    instructionsOrder.add(newInstructionIndices)
                    mutatedClassTracker.targetMutation.instructionsOrder = instructionsOrder
                }
            } else {
                if (mutatedClassTracker.originalOpcode != null && opcode == mutatedClassTracker.originalOpcode) {
                    if (!instructionsOrder.contains(newInstructionIndices)) {
                        instructionsOrder.add(newInstructionIndices)
                    }
                }
            }
        }
    }

    /**
     * Is target mutation
     *
     * @param newMutation
     * @return
     */
    fun isTargetMutation(newMutation: Mutation?): Boolean {
        return newMutation?.mutationID == mutatedClassTracker.targetMutation!!.mutationID
    }
}