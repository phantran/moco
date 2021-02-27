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
import io.moco.utils.JavaInfo
import io.moco.utils.MoCoLogger
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

    open fun createUniqueID(prefix: String, op: Int): String {
        return "${prefix}_${opcodeDesc[op]?.second}"
    }
}