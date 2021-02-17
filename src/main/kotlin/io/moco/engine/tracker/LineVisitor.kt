package io.moco.engine.tracker

import io.moco.utils.ASMInfoUtil
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor


class LineVisitor(
    delegateMethodVisitor: MethodVisitor?,
    private val mutatedMethodTracker: MutatedMethodTracker,
) :
    MethodVisitor(ASMInfoUtil.ASM_VERSION, delegateMethodVisitor) {

    override fun visitLineNumber(line: Int, start: Label) {
        mutatedMethodTracker.currMutatedLineNumber = line
        mv.visitLineNumber(line, start)
    }

}
