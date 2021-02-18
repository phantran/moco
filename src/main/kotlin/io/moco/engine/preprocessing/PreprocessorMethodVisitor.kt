package io.moco.engine.preprocessing

import io.moco.engine.tracker.Block
import io.moco.utils.ASMInfoUtil
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode


class PreprocessorMethodVisitor(
    val parent: PreprocessorClassVisitor, val className: String,
    val mv: MethodVisitor?, access: Int,
    val name: String?, desc: String?, signature: String?,
    exceptions: Array<String?>?
) : MethodNode(ASMInfoUtil.ASM_VERSION, access, name, desc, signature, exceptions) {

    override fun visitEnd() {
        // collect blocks info
        val blocks: MutableList<Block> = MethodAnalyser.analyse(this)
        // TODO: record blocks of method here
        PreprocessorTracker.registerBlock(className, blocks)

        // call method of preprocessorTracker to register cut name to test
        mv.visitLdcInsn(className)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, PreprocessorTracker.internalClsName,
            "registerCUT", "(Ljava/lang/String;)V", false)

        accept(mv)
    }
}
