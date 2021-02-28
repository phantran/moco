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

package io.moco.utils

import io.moco.engine.MocoAgent
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

class JarUtil {
    companion object {
        @Throws(IOException::class)
        fun createTemporaryAgentJar(): String? {
            return try {
                val jarName = File.createTempFile(
                    System.currentTimeMillis()
                        .toString() + ("" + Math.random()).replace("\\.".toRegex(), ""),
                    ".jar"
                )

                val outputStream = FileOutputStream(jarName)
                val jarFile = File(jarName.absolutePath)
                val manifest = Manifest()
                manifest.clear()
                val temp = manifest.mainAttributes
                if (temp.getValue(Attributes.Name.MANIFEST_VERSION) == null) {
                    temp[Attributes.Name.MANIFEST_VERSION] = "1.0"
                }
                temp.putValue(
                    "Boot-Class-Path",
                    jarFile.absolutePath.replace('\\', '/')
                )
                temp.putValue(
                    "Agent-Class",
                    MocoAgent::class.java.name
                )
                temp.putValue("Can-Redefine-Classes", "true")
                temp.putValue("Can-Retransform-Classes", "true")
                temp.putValue(
                    "Premain-Class",
                    MocoAgent::class.java.name
                )
                temp.putValue("Can-Set-Native-Method-Prefix", "true")
                JarOutputStream(outputStream, manifest).use {}

                jarName.absolutePath
            } catch (e: IOException) {
                throw e
            }
        }

        fun removeTemporaryAgentJar(location: String?) {
            if (location != null) {
                val f = File(location)
                f.delete()
            }
        }
    }
}