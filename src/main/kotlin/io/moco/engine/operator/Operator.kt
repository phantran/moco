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
        val supportedOperatorNames = listOf("AOR", "ROR", "LCR", "UOI")

        fun nameToOperator(it: String): Operator? {
            return mapping[it]
        }
        private val mapping: Map<String, Operator> = mapOf(
            "AOR" to ReplacementOperator("AOR"), // Arithmetic operator replacement
            "ROR" to ReplacementOperator("ROR"), // Relational operator replacement
            "LCR" to ReplacementOperator("LCR"), // Logical connector replacement
            "UOI" to InsertionOperator("UOI") // Unary operator insertion
        )
    }

    fun generateVisitor(
        tracker: MutatedMethodTracker,
        methodInfo: MethodInfo,
        delegateMethodVisitor: MethodVisitor
    ): MethodVisitor?
}