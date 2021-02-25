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


package io.moco.engine

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type


data class MethodInfo(
    val enclosingClass: ClassInfo?, val access: Int,
    val name: String, val methodDescriptor: String
) {
    val description: String
        get() = enclosingClass?.name + "::" + name

    val isStatic: Boolean
        get() = ((access and Opcodes.ACC_STATIC) != 0)

    val isSynthetic: Boolean
        get() = ((access and Opcodes.ACC_SYNTHETIC) != 0)

    val isConstructor: Boolean
        get() = isConstructor(name)

    val returnType: Type
        get() = Type.getReturnType(methodDescriptor)

    val isStaticInitializer: Boolean
        get() = ("<clinit>" == name)

    val isVoid: Boolean
        get() = isVoid(methodDescriptor)

    fun takesNoParameters(): Boolean {
        return methodDescriptor.startsWith("()")
    }

    val isGeneratedEnumMethod: Boolean
        get() = (enclosingClass?.isEnum ?: false
                && (isValuesMethod || isValueOfMethod || isStaticInitializer))

    private val isValuesMethod: Boolean
        get() {
            return (name == "values") && takesNoParameters() && isStatic
        }
    private val isValueOfMethod: Boolean
        get() {
            return ((name == "valueOf") && methodDescriptor.startsWith("(Ljava/lang/String;)")
                    && isStatic)
        }

    companion object {
        fun isConstructor(methodName: String): Boolean {
            return ("<init>" == methodName)
        }

        fun isVoid(desc: String?): Boolean {
            return (Type.getReturnType(desc) == Type.VOID_TYPE)
        }
    }
}

