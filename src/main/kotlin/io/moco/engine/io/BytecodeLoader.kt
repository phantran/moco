package io.moco.engine.io

import java.io.*
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.channels.Channels


class BytecodeLoader(cp: String?) {
    private val clsPaths: Set<File>

    init {
        if (cp == null) {
            clsPaths = initClsPath()
        } else {
            clsPaths = cp.split(",").map { File(it.trim()) }.toSet()
        }
    }

    private fun initClsPath(): Set<File> {
        val res = mutableSetOf<File>()
        val classPath = System.getProperty("java.class.path")
        val separator = File.pathSeparator
        var temp = setOf<String>()
        if (classPath != null) {
            temp = classPath.split(separator.toRegex()).toSet()
        }

        for (item in temp) {
            res.add(getCanonicalPath(item))
        }

        try {
            res.filter{ it.exists() && it.canRead() && it.isDirectory }
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


    @Throws(IOException::class)
    fun getByteCodeArray(classname: String?): ByteArray? {
        for (root in this.clsPaths) {
            val fn = classname?.replace('.', File.separatorChar) + ".class"
            val f = File(root, fn)
            if (f.exists() && f.canRead()) {
                return streamToByteArray(FileInputStream(f))
            }
            //TODO: Add support for jar file later and logging action here
        }
        return null
    }


    @Throws(IOException::class)
    fun streamToByteArray(inp: InputStream?): ByteArray? {
        ByteArrayOutputStream().use { result ->
            if (inp != null) {
                val src = Channels.newChannel(inp)
                val dst = Channels.newChannel(result)
                val buffer = ByteBuffer.allocateDirect(16 * 1024)
                while (src.read(buffer) != -1) {
                    buffer.flip()
                    dst.write(buffer)
                    buffer.compact()
                }
                buffer.flip()
                while (buffer.hasRemaining()) {
                    dst.write(buffer)
                }
            }
            return result.toByteArray()
        }
    }
}


