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

package io.github.phantran.engine.mutator.deletion

import io.github.phantran.engine.MethodInfo
import io.github.phantran.engine.tracker.MutatedMethodTracker
import io.github.phantran.utils.JavaInfo
import io.github.phantran.utils.MoCoLogger
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.LocalVariablesSorter

open class DeletionMutator(
    val tracker: MutatedMethodTracker,
    methodInfo: MethodInfo,
    delegateMethodVisitor: MethodVisitor
) : LocalVariablesSorter(JavaInfo.ASM_VERSION, methodInfo.access, methodInfo.methodDescriptor, delegateMethodVisitor) {
    val logger = MoCoLogger()
    open val opcodeDesc: Map<Int, Pair<String, String>> = mapOf()
    open val supportedOpcodes: Map<String, List<Int>> = mapOf()

    open fun createDesc(action: String, op: Int): String {
        return "$action ${opcodeDesc[op]?.first}"
    }

    open fun createUniqueID(op: Int, position: String): String {
        // Unique ID format is important since it is used in many other places such as
        // originalOpcode and other third party plugins, it should always start with opcode
        return "${opcodeDesc[op]?.second}-${position}-${tracker.currConsideredLineNumber}"
    }
}