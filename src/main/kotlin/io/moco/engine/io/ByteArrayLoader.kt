package io.moco.engine.io

import java.io.*
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.channels.Channels


class ByteArrayLoader(cp: String?) {
    private val clsPaths: Set<File>

    init {
        clsPaths = cp?.split(",")?.map { File(it.trim()) }?.toSet() ?: initClsPath()
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
    fun getByteArray(className: String?): ByteArray? {
        for (root in this.clsPaths) {
            val fn = className?.replace('.', File.separatorChar) + ".class"
            val f = File(root, fn)
            if (f.exists() && f.canRead()) {
                return streamToByteArr(FileInputStream(f))
            }
            //TODO: Add support for jar file later and logging action here
        }
        return null
    }


    @Throws(IOException::class)
    fun streamToByteArr(inp: InputStream?): ByteArray? {
        ByteArrayOutputStream().use { output ->
            if (inp != null) {
                val inputChannel = Channels.newChannel(inp)
                val outputChannel = Channels.newChannel(output)
                val buffer = ByteBuffer.allocateDirect(16 * 1024)
                while (inputChannel.read(buffer) != -1) {
                    buffer.flip()
                    outputChannel.write(buffer)
                    buffer.compact()
                }
                buffer.flip()
                while (buffer.hasRemaining()) {
                    outputChannel.write(buffer)
                }
            }
            return output.toByteArray()
        }
    }
}


