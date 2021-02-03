package io.moco.engine.mutator.replacement

import org.objectweb.asm.MethodVisitor


class ReplaceOperation(val replacementOpcode: Int, val message: String) {
    fun accept(mv: MethodVisitor) {
        mv.visitInsn(replacementOpcode)
    }
}
