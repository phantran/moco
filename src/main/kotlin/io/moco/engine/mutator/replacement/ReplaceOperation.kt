package io.moco.engine.mutator.replacement

import org.objectweb.asm.MethodVisitor


class ReplaceOperation(val newOpcode: Int, val message: String) {
    fun mutate(mv: MethodVisitor) {
        mv.visitInsn(newOpcode)
    }
}
