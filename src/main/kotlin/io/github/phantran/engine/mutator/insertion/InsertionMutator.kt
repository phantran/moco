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
import io.github.phantran.utils.JavaInfo
import io.github.phantran.utils.MoCoLogger
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor


open class InsertionMutator(
    val operator: InsertionOperator,
    val tracker: MutatedMethodTracker,
    delegateMethodVisitor: MethodVisitor
) : MethodVisitor(JavaInfo.ASM_VERSION, delegateMethodVisitor) {

    val logger = MoCoLogger()


    open val opcodeDesc: Map<Int, Pair<String, String>> = mapOf()

    open val supportedOpcodes: Map<String, List<Int>> = mapOf()

    open fun createDesc(action: String, op: Int): String {
        return "$action ${opcodeDesc[op]?.first}"
    }

    open fun createUniqueID(op: Int, type: String, incOrDec: String ): String {
        // Unique ID format is important since it is used in many other places such as
        // originalOpcode and other third party plugins, it should always start with opcode
        return "${opcodeDesc[op]?.second}-${type}UOI-$incOrDec-${tracker.currConsideredLineNumber}"
    }

    protected var varIndexToLineNo: Pair<Int, Int>? = null
    private var collectVarName: String? = null
    private val labelsToLineNoMap: MutableMap<Label, Int> = mutableMapOf()

    override fun visitLabel(label: Label?) {
        if (label != null) labelsToLineNoMap[label] = tracker.currConsideredLineNumber
        mv.visitLabel(label)
    }

    override fun visitLocalVariable(
        name: String?,
        descriptor: String?,
        signature: String?,
        start: Label?,
        end: Label?,
        index: Int
    ) {
        if (collectVarName == null && varIndexToLineNo != null) {
            if (varIndexToLineNo!!.first == index) {
                val startLine = labelsToLineNoMap[start]
                val endLine = labelsToLineNoMap[end]
                if (startLine != null && endLine != null) {
                    // Record line number of mutated line is within the record local variable scope
                    if (startLine < varIndexToLineNo!!.second && varIndexToLineNo!!.second < endLine) {
                        collectVarName = name
                        val m = tracker.mutatedClassTracker.targetMutation
                        if (name != null) {
                            m!!.additionalInfo["varName"] = name
                        }
                    }
                }
            }
        }
        mv.visitLocalVariable(name, descriptor, signature, start, end, index)
    }
}