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

package io.github.phantran.engine

import org.objectweb.asm.Opcodes

data class MethodInfo(
    val enclosingClass: ClassInfo?, val access: Int,
    val name: String, val methodDescriptor: String
) {
    val description: String
        get() = enclosingClass?.name + "::" + name

    private val isStatic: Boolean
        get() = ((access and Opcodes.ACC_STATIC) != 0)

    private val isStaticInitializer: Boolean
        get() = ("<clinit>" == name)

    private fun takesNoParameters(): Boolean {
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
}

