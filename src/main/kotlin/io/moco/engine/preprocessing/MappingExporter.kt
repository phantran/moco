package io.moco.engine.preprocessing

import java.io.*

/**
 * Mapping exporter
 * This class is responsible for exporting the map of class names to test class names to an XML file
 * The exported XML file will be read in each execution to retrieve this mapping information
 * @constructor Create empty Mapping exporter
 *
 * @param baseDir
 */
class MappingExporter(baseDir: String) {

    private var storeDir: File? = null

    init {
        val temp = File(baseDir)
        if (!temp.exists()) {
            temp.mkdirs()
        }
        storeDir = temp
    }

    /**
     * Save mapping, mapping a map of class under test to test classes
     *
     * @param mappings
     */
    fun saveMapping(mappings: MutableMap<String, List<String>>) {
        try {
            val outWriter = createWriter("testmapping.xml")
            write(outWriter, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            write(outWriter, "<coverage>\n")
            mappings.map { writeMapping(it, outWriter) }
            write(outWriter, "</coverage>\n")
            outWriter.close()
        } catch (e: IOException) {
            throw RuntimeException("Error while creating test mapping file")
        }
    }

    /**
     * Create writer
     *
     * @param file
     * @return
     */
    fun createWriter(file: String): Writer {
        try {
            val fw = FileWriter(storeDir!!.absolutePath + File.separatorChar + file)
            return BufferedWriter(fw)
        } catch (ex: IOException) {
            throw RuntimeException("Error while creating test mapping file")
        }
    }

    private fun writeMapping(entry: Map.Entry<String, List<String>>, writer: Writer) {
        write(writer, "<entry classname='" + entry.key + "'>\n")
        write(writer, "<tests>\n")
        val tests: List<String> = entry.value
        tests.sorted()
        for (test: String in tests) {
            write(writer, "<test name='" + escapeHtmlChars(test) + "'/>\n")
        }
        write(writer, "</tests>\n")
        write(writer, "</entry>\n")
    }

    private fun write(out: Writer, value: String) {
        try {
            out.write(value)
        } catch (e: IOException) {
            throw RuntimeException("Error while creating test mapping file")
        }
    }

    /**
     * Escape html chars
     *
     * @param s
     * @return
     */
    fun escapeHtmlChars(s: String): String {
        val stringBuilder = StringBuilder()
        for (element in s) {
            val v = element.toInt()
            if (v == 0) {
                stringBuilder.append("\\0")
                continue
            }
            if (v < 32 || v > 127 || v == 38 || v == 39 || v == 60 || v == 62 || v == 34) {
                stringBuilder.append('&')
                stringBuilder.append('#')
                stringBuilder.append(v)
                stringBuilder.append(';')
            } else {
                stringBuilder.append(element)
            }
        }
        return stringBuilder.toString()
    }
}