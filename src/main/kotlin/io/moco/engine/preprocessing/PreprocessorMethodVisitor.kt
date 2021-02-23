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
