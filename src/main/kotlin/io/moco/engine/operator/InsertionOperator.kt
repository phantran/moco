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
import io.moco.engine.mutator.insertion.UOIPO
import io.moco.engine.mutator.insertion.UOIPR
import io.moco.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.MethodVisitor


class InsertionOperator(override val operatorName: String): Operator {

    override fun generateVisitor(
        tracker: MutatedMethodTracker,
        methodInfo: MethodInfo,
        delegateMethodVisitor: MethodVisitor
    ): MethodVisitor? {
        return when (operatorName) {
            "UOI" -> UOIPR(this, tracker, UOIPO(this, tracker, delegateMethodVisitor))
            else -> null
        }
    }
}