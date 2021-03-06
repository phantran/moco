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

import io.github.phantran.engine.ClassInfo
import io.github.phantran.engine.ClassName
import io.github.phantran.engine.MethodInfo
import io.github.phantran.engine.MethodName
import io.github.phantran.engine.operator.Operator
import io.github.phantran.engine.tracker.MutatedClassTracker
import io.github.phantran.engine.tracker.MutatedMethodTracker
import io.github.phantran.utils.JavaInfo
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import java.util.HashSet


class MutatedClassVisitor(
    delegateClassVisitor: ClassVisitor?, val tracker: MutatedClassTracker,
    val filter: List<String> = mutableListOf(), operators: List<Operator>?, private val collectingPhase: Boolean = false
) : ClassVisitor(JavaInfo.ASM_VERSION, delegateClassVisitor) {

    private val chosenOperators: MutableSet<Operator> = HashSet<Operator>()

    init {
        chosenOperators.addAll(operators!!)
    }

    override fun visit(
        version: Int, access: Int, name: String,
        signature: String?, superName: String, interfaces: Array<String>
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        tracker.setClsInfo(
            ClassInfo(
                version, access, name, signature,
                superName, interfaces
            )
        )
    }

    override fun visitSource(source: String, debug: String?) {
        super.visitSource(source, debug)
        tracker.setFileName(source)
    }

    override fun visitMethod(
        access: Int, methodName: String,
        methodDescriptor: String, signature: String?,
        exceptions: Array<String>?
    ): MethodVisitor {

        val clsInfo = tracker.getClsInfo()
        val methodTracker = MutatedMethodTracker(
            tracker, MutatedMethodLocation(
                clsInfo?.let { ClassName.fromString(it.name) },
                MethodName(methodName), methodDescriptor
            )
        )

        val methodVisitor = cv.visitMethod(
            access, methodName,
            methodDescriptor, signature, exceptions
        )

        val info = MethodInfo(tracker.getClsInfo(), access, methodName, methodDescriptor)
        //TODO: support method exclusion here
        return if (!filter.contains(info.name)) {
            var chain = methodVisitor
            for (each in chosenOperators) {
                chain = each.generateVisitor(methodTracker, info, chain)
            }
            // Use loop visitor to detect loop (for, while, do while)
            if (collectingPhase) {
                chain = FilterVisitor(chain, methodTracker)
            }
            val wrapped = LineVisitor(chain, methodTracker)
            InstructionVisitor(wrapped, methodTracker)
        } else {
            methodVisitor
        }
    }
}