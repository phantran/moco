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

import io.github.phantran.engine.ClassName
import io.github.phantran.utils.MoCoLogger
import org.objectweb.asm.*;
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.objectweb.asm.util.TraceClassVisitor
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.IllegalClassFormatException
import java.security.ProtectionDomain
import java.io.PrintWriter


/**
 * Preprocessor transformer
 *
 * @property filteredSourceClasses
 * @constructor Create empty Preprocessor transformer
 */
class PreprocessorTransformer(private val filteredSourceClasses: Set<ClassName>) : ClassFileTransformer {
    private val logger = MoCoLogger()

    @Throws(IllegalClassFormatException::class)
    override fun transform(
        loader: ClassLoader, className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain, classfileBuffer: ByteArray
    ): ByteArray? {
        return if (isTargetCUT(className)) {
            try {
                val cr = ClassReader(classfileBuffer)
                val cw = ClassWriter(cr, COMPUTE_MAXS);
//                val cv = TraceClassVisitor(cw, PrintWriter(System.out))
                cr.accept(
                    PreprocessorClassVisitor(cw),
                    ClassReader.EXPAND_FRAMES
                )
                cw.toByteArray()

            } catch (t: RuntimeException) {
                logger.error("Error while transforming and preprocessing $className")
                t.printStackTrace()
                return null
            }
        } else {
            null
        }
    }

    private fun isTargetCUT(className: String): Boolean {
        return filteredSourceClasses.any { it.name == className }
    }
}