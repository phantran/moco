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


package io.moco.engine.operator

import io.moco.engine.MethodInfo
import io.moco.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.MethodVisitor


interface Operator {
    val operatorName: String

    companion object {
        val supportedOperatorNames = listOf("AOD", "BLD", "POUOI", "PRUOI", "AOR", "BLR", "ROR")

        fun nameToOperator(it: String): Operator? {
            return mapping[it]
        }
        private val mapping: Map<String, Operator> = mapOf(
            "AOD" to DeletionOperator("AOD"), // Arithmetic operator deletion
            "BLD" to DeletionOperator("BLD"), // Bitwise logical deletion
            "POUOI" to InsertionOperator("POUOI"), // Post unary operator insertion
            "PRUOI" to InsertionOperator("PRUOI"), // Post pre operator insertion
            "AOR" to ReplacementOperator("AOR"), // Arithmetic operator replacement
            "BLR" to ReplacementOperator("BLR"), // Bitwise logical replacement
            "ROR" to ReplacementOperator("ROR"), // Relational operator replacement
        )
    }

    fun generateVisitor(
        tracker: MutatedMethodTracker,
        methodInfo: MethodInfo,
        delegateMethodVisitor: MethodVisitor
    ): MethodVisitor?

    fun notReachedTypeQuantityLimit(mutationLimitTracker: MutableMap<Int, MutableSet<String>>?, loc: Int): Boolean {
        if (mutationLimitTracker != null) {
            if (loc in mutationLimitTracker.keys && mutationLimitTracker[loc]?.contains(operatorName) == true) {
                return false
            }
        }
        // null mutationLimitTracker implies limitation parameter was set to false or we are in mutation test phase
        return true
    }
}