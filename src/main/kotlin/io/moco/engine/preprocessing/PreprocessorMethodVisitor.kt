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

import io.moco.utils.JavaInfo
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode


class PreprocessorMethodVisitor(
    val className: String, val mv: MethodVisitor?,
    access: Int, val name: String?,
    desc: String?, signature: String?, exceptions: Array<String?>?
) : MethodNode(JavaInfo.ASM_VERSION, access, name, desc, signature, exceptions) {

    override fun visitEnd() {
        // call method of preprocessorTracker to register cut name to test
        mv.visitLdcInsn(className)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, PreprocessorTracker.internalClsName,
            "registerCUT", "(Ljava/lang/String;)V", false)
        accept(mv)
    }

    override fun visitLineNumber(line: Int, start: Label) {
        // call method of preprocessorTracker to register line
        mv.visitLdcInsn(className)
        if (line < 128) {
            mv.visitIntInsn(Opcodes.BIPUSH, line)
        } else {
            mv.visitIntInsn(Opcodes.SIPUSH, line)
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, PreprocessorTracker.internalClsName,
            "registerLine", "(Ljava/lang/String;I)V", false)
        mv.visitLineNumber(line, start)
    }
}
