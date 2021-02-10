package io.moco.engine.preprocessing

import io.moco.engine.tracker.Block
import java.io.*

/**
 * Preprocess Exporter
 * This class is responsible for exporting the map of class names to test class names to an XML file
 * The exported XML file will be read in each execution to retrieve this mapping information
 * @constructor Create empty Mapping exporter
 *
 * @param dir
 */
class PreprocessExporter(dir: String) {

    private var storeDir: File? = null

    init {
        val temp = File("$dir/moco/preprocess/")
        if (!temp.exists()) {
            temp.mkdirs()
        }
        this.storeDir = temp
    }

    /**
     * Save preprocess result
     *
     * @param results
     */
    fun savePreprocessResult(results: List<PreprocessClassResult>) {
        try {
            val outWriter = createWriter()
            write(outWriter, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            write(outWriter, "<preprocess_result>\n")
            results.map { writeResult(it, outWriter) }
            write(outWriter, "</preprocess_result>\n")
            outWriter.close()
        } catch (e: IOException) {
            throw RuntimeException("Error while saving result to preprocessing file")
        }
    }

    /**
     * Create writer
     *
     * @param file
     * @return
     */
    private fun createWriter(file: String="storage.xml"): Writer {
        try {
            val fw = FileWriter(storeDir!!.absolutePath + File.separatorChar + file)
            return BufferedWriter(fw)
        } catch (ex: IOException) {
            throw RuntimeException("Error while saving result to preprocessing file")
        }
    }

    private fun writeResult(entry: PreprocessClassResult, writer: Writer) {
        write(writer, "<entry classname='" + entry.classUnderTestName + "'>\n")
        write(writer, "<tests>\n")
        val tests: List<String> = entry.testClassesNames
        tests.sorted()
        for (test: String in tests) {
            write(writer, "<test name='" + escapeHtmlChars(test) + "'/>\n")
        }
        write(writer, "</tests>\n")

        write(writer, "<blocks>\n")
        val blocks: List<Block>? = entry.blockLists
        blocks?.sortedBy { it.getFstIns() }
        blocks?.forEachIndexed { index, item ->
            write(writer, "<block id='$index' first_ins='${item.getFstIns()}' " +
                    "last_ins='${item.getLstIns()}' 'lines=${
                        item.getLines().joinToString { it.toString() }
                    }}'/>\n"
            )
        }
        write(writer, "</blocks>\n")
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
    private fun escapeHtmlChars(s: String): String {
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