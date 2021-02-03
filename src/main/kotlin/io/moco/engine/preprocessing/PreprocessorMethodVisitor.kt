package io.moco.engine.preprocessing

import io.moco.engine.tracker.Block
import io.moco.utils.ASMInfoUtil
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode


class PreprocessorMethodVisitor(
    val parent: PreprocessorClassVisitor, val className: String,
    val mv: MethodVisitor?, val access: Int,
    val name: String?, val desc: String?, val signature: String?,
    exceptions: Array<String?>?
) : MethodNode(ASMInfoUtil.ASM_VERSION, access, name, desc, signature, exceptions) {

    override fun visitCode() {
        mv.visitCode()
        // collect blocks info
        val blocks: List<Block> = MethodAnalyser.analyse(this)
        // TODO: record blocks of method here
        PreprocessorTracker.registerBlock(className, blocks)

        // call method of preprocessorTracker to register cut name to test
        mv.visitLdcInsn(className)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, PreprocessorTracker::class.java.name,
            "registerCUT", "(Ljava/lang/String;)V", false)
    }
}
