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


package io.github.phantran.engine.preprocessing

import io.github.phantran.utils.JavaInfo
import org.objectweb.asm.*

class PreprocessorClassVisitor(cw: ClassVisitor?) :
    ClassVisitor(JavaInfo.ASM_VERSION, cw) {

    private var className: String? = null
    private var isInterface: Boolean = false
    private var isAbstractClass: Boolean = false
    private var isSynthetic: Boolean = false

    override fun visit(
        version: Int, access: Int, name: String?, signature: String?,
        superName: String?, interfaces: Array<String?>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        if (access and Opcodes.ACC_INTERFACE != 0) isInterface = true
        if (access and Opcodes.ACC_ABSTRACT != 0) isAbstractClass = true
        if (access and Opcodes.ACC_SYNTHETIC != 0) isSynthetic = true
        className = name
    }

    override fun visitMethod(
        access: Int, name: String?,
        desc: String?, signature: String?, exceptions: Array<String?>?
    ): MethodVisitor {
        val methodVisitor = cv.visitMethod(
            access, name, desc,
            signature, exceptions
        )

        return if (!isSyntheticMethod(access) && name != "<clinit>" &&
            !isAbstractClass && !isInterface && !isSynthetic
        ) {
            PreprocessorMethodVisitor(className!!, methodVisitor, access, name, desc, signature, exceptions)
        } else {
            methodVisitor
        }
    }

    private fun isSyntheticMethod(access: Int): Boolean {
        return access and Opcodes.ACC_BRIDGE != 0
    }
}