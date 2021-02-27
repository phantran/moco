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
import io.moco.engine.mutator.deletion.AOD
import io.moco.engine.mutator.deletion.BLD
import io.moco.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.MethodVisitor


class DeletionOperator(override val operatorName: String): Operator {

    override fun generateVisitor(
        tracker: MutatedMethodTracker,
        methodInfo: MethodInfo,
        delegateMethodVisitor: MethodVisitor
    ): MethodVisitor? {
        return when (operatorName) {
            "AOD" -> AOD(this, tracker, methodInfo, delegateMethodVisitor)
            "BLD" -> BLD(this, tracker, methodInfo, delegateMethodVisitor)
            else -> null
        }
    }
}