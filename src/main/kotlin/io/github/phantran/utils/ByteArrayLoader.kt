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

package io.github.phantran.utils

import java.io.*
import java.lang.RuntimeException
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.zip.ZipFile

class ByteArrayLoader(cp: String?) {
    private val clsPaths: Set<File>
    private val logger = MoCoLogger()

    init {
        clsPaths = cp?.split(File.pathSeparator)?.map { File(it.trim()) }?.toSet() ?: initClsPath()
    }

    private fun initClsPath(): Set<File> {
        val res = mutableSetOf<File>()
        val classPath = System.getProperty("java.class.path")
        var temp = setOf<String>()
        if (classPath != null) {
            temp = classPath.split(File.pathSeparator.toRegex()).toSet()
        }
        for (item in temp) {
            res.add(getCanonicalPath(item))
        }
        try {
            res.filter { it.exists() && it.canRead() && it.isDirectory }
        } catch (e: IOException) {
            e.printStackTrace()
            throw IOException("Error handling while initializing classpath of bytecode loader")
        }
        return res
    }

    private fun getCanonicalPath(path: String): File {
        try {
            return File(path).canonicalFile
        } catch (ex: IOException) {
            throw RuntimeException(
                "Error getting canonical path of classpath: $path", ex
            )
        }
    }

    @Throws(Exception::class)
    fun getByteArray(className: String?): ByteArray? {
        try {
            val fn = className?.replace('.', File.separatorChar) + ".class"
            for (root in this.clsPaths) {
                if (root.isDirectory) {
                    val f = File(root, fn)
                    if (f.exists() && f.canRead()) {
                        return streamToByteArr(FileInputStream(f))
                    }
                } else if (root.isFile) {
                    val temp = ZipFile(root)
                    val entry = temp.getEntry(fn) ?: continue
                    return streamToByteArr(temp.getInputStream(entry))
                }
            }
            return null
        } catch (e: Exception) {
            logger.error(e.printStackTrace().toString())
            throw e
        }
    }

    @Throws(IOException::class)
    fun streamToByteArr(inp: InputStream?): ByteArray? {
        ByteArrayOutputStream().use { output ->
            if (inp != null) {
                val inputChannel = Channels.newChannel(inp)
                val outputChannel = Channels.newChannel(output)
                val buffer = ByteBuffer.allocateDirect(16 * 1024)
                while (inputChannel.read(buffer) != -1) {
                    (buffer as Buffer).flip()
                    outputChannel.write(buffer)
                    buffer.compact()
                }
                (buffer as Buffer).flip()
                while (buffer.hasRemaining()) {
                    outputChannel.write(buffer)
                }
            }
            return output.toByteArray()
        }
    }
}