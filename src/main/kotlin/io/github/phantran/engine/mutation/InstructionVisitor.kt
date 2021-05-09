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

import io.github.phantran.engine.tracker.MutatedMethodTracker
import io.github.phantran.utils.JavaInfo
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor


class InstructionVisitor(
    mv: MethodVisitor?,
    private val mutatedMethodTracker: MutatedMethodTracker
) : MethodVisitor(JavaInfo.ASM_VERSION, mv) {

    override fun visitFrame(
        type: Int, nLocal: Int,
        local: Array<Any>, nStack: Int, stack: Array<Any>
    ) {
        mutatedMethodTracker.instructionIndex++
        super.visitFrame(type, nLocal, local, nStack, stack)
    }

    override fun visitInsn(opcode: Int) {
        mutatedMethodTracker.instructionIndex++
        super.visitInsn(opcode)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        mutatedMethodTracker.instructionIndex++
        super.visitIntInsn(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        mutatedMethodTracker.instructionIndex++
        super.visitVarInsn(opcode, `var`)
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        mutatedMethodTracker.instructionIndex++
        super.visitTypeInsn(opcode, type)
    }

    override fun visitFieldInsn(
        opcode: Int, owner: String,
        name: String, desc: String
    ) {
        mutatedMethodTracker.instructionIndex++
        super.visitFieldInsn(opcode, owner, name, desc)
    }

    override fun visitMethodInsn(
        opcode: Int, owner: String,
        name: String, desc: String, itf: Boolean
    ) {
        mutatedMethodTracker.instructionIndex++
        super.visitMethodInsn(opcode, owner, name, desc, itf)
    }

    override fun visitInvokeDynamicInsn(
        name: String, desc: String,
        bsm: Handle, vararg bsmArgs: Any
    ) {
        mutatedMethodTracker.instructionIndex++
        super.visitInvokeDynamicInsn(name, desc, bsm, *bsmArgs)
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        mutatedMethodTracker.instructionIndex++
        super.visitJumpInsn(opcode, label)
    }

    override fun visitLabel(label: Label) {
        mutatedMethodTracker.instructionIndex++
        super.visitLabel(label)
    }

    override fun visitLdcInsn(cst: Any) {
        mutatedMethodTracker.instructionIndex++
        super.visitLdcInsn(cst)
    }

    override fun visitIincInsn(`var`: Int, increment: Int) {
        mutatedMethodTracker.instructionIndex++
        super.visitIincInsn(`var`, increment)
    }

    override fun visitTableSwitchInsn(
        min: Int, max: Int,
        dflt: Label, vararg labels: Label
    ) {
        mutatedMethodTracker.instructionIndex++
        super.visitTableSwitchInsn(min, max, dflt, *labels)
    }

    override fun visitLookupSwitchInsn(
        dflt: Label, keys: IntArray,
        labels: Array<Label>
    ) {
        mutatedMethodTracker.instructionIndex++
        super.visitLookupSwitchInsn(dflt, keys, labels)
    }

    override fun visitMultiANewArrayInsn(desc: String, dims: Int) {
        mutatedMethodTracker.instructionIndex++
        super.visitMultiANewArrayInsn(desc, dims)
    }

    override fun visitLineNumber(line: Int, start: Label) {
        mutatedMethodTracker.instructionIndex++
        super.visitLineNumber(line, start)
    }

}