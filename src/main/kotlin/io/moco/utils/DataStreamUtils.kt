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

import java.io.*


object DataStreamUtils {
    @Throws(IOException::class)
    fun writeObject(outputStream: DataOutputStream, value: Serializable) {
        try {
            ByteArrayOutputStream().use { bos ->
                val out: ObjectOutput = ObjectOutputStream(bos)
                out.writeObject(value)
                out.flush()
                val temp = bos.toByteArray()
                outputStream.writeInt(temp.size)
                outputStream.write(temp)
            }
        } catch (e: IOException) {
            throw e
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Serializable?> readObject(dis: DataInputStream): T {
        return try {
            val length: Int = dis.readInt()
            val data = ByteArray(length)
            dis.readFully(data)
            deserialize(data) as T
        } catch (e: IOException) {
            throw e
        }
    }

    @Throws(IOException::class)
    private fun deserialize(bytes: ByteArray): Any? {
        val bis = ByteArrayInputStream(bytes)
        try {
            ObjectInputStream(bis).use { return it.readObject() }
        } catch (e: ClassNotFoundException) {
            throw e
        }
    }
}