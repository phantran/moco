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

package io.moco.engine.mutation

import io.moco.engine.tracker.MutatedMethodTracker
import io.moco.utils.JavaInfo
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor

class LineVisitor(
    delegateMethodVisitor: MethodVisitor?,
    private val mutatedMethodTracker: MutatedMethodTracker,
) :
    MethodVisitor(JavaInfo.ASM_VERSION, delegateMethodVisitor) {

    override fun visitLineNumber(line: Int, start: Label) {
        mutatedMethodTracker.currConsideredLineNumber = line
        mutatedMethodTracker.currConsideredLineLabel = start
        mv.visitLineNumber(line, start)
    }

}