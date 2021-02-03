package io.moco.engine.tracker

import io.moco.utils.ASMInfoUtil
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor


class InstructionVisitor(
    mv: MethodVisitor?,
    private val count: InstructionTracker
) : MethodVisitor(ASMInfoUtil.ASM_VERSION, mv) {

    override fun visitFrame(
        type: Int, nLocal: Int,
        local: Array<Any>, nStack: Int, stack: Array<Any>
    ) {
        count.increment()
        super.visitFrame(type, nLocal, local, nStack, stack)
    }

    override fun visitInsn(opcode: Int) {
        count.increment()
        super.visitInsn(opcode)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        count.increment()
        super.visitIntInsn(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        count.increment()
        super.visitVarInsn(opcode, `var`)
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        count.increment()
        super.visitTypeInsn(opcode, type)
    }

    override fun visitFieldInsn(
        opcode: Int, owner: String,
        name: String, desc: String
    ) {
        count.increment()
        super.visitFieldInsn(opcode, owner, name, desc)
    }

    override fun visitMethodInsn(
        opcode: Int, owner: String,
        name: String, desc: String, itf: Boolean
    ) {
        count.increment()
        super.visitMethodInsn(opcode, owner, name, desc, itf)
    }

    override fun visitInvokeDynamicInsn(
        name: String, desc: String,
        bsm: Handle, vararg bsmArgs: Any
    ) {
        count.increment()
        super.visitInvokeDynamicInsn(name, desc, bsm, *bsmArgs)
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        count.increment()
        super.visitJumpInsn(opcode, label)
    }

    override fun visitLabel(label: Label) {
        count.increment()
        super.visitLabel(label)
    }

    override fun visitLdcInsn(cst: Any) {
        count.increment()
        super.visitLdcInsn(cst)
    }

    override fun visitIincInsn(`var`: Int, increment: Int) {
        count.increment()
        super.visitIincInsn(`var`, increment)
    }

    override fun visitTableSwitchInsn(
        min: Int, max: Int,
        dflt: Label, vararg labels: Label
    ) {
        count.increment()
        super.visitTableSwitchInsn(min, max, dflt, *labels)
    }

    override fun visitLookupSwitchInsn(
        dflt: Label, keys: IntArray,
        labels: Array<Label>
    ) {
        count.increment()
        super.visitLookupSwitchInsn(dflt, keys, labels)
    }

    override fun visitMultiANewArrayInsn(desc: String, dims: Int) {
        count.increment()
        super.visitMultiANewArrayInsn(desc, dims)
    }

    override fun visitLineNumber(line: Int, start: Label) {
        count.increment()
        super.visitLineNumber(line, start)
    }

}