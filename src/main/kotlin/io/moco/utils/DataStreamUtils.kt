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