package io.moco.utils

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.moco.engine.preprocessing.PreprocessStorage
import java.io.File
import java.io.IOException

/**
 * Preprocess Converter
 * This class is responsible for exporting the map of class names to test class names to an JSON file
 * The exported JSON file will be read in each execution to retrieve this mapping information
 * It is also responsible for importing json file to create object of the PreprocessStorage class
 * @constructor Create empty Mapping exporter
 *
 * @param dir
 */
class JsonConverter(private val dir: String, private val fileName: String) {

    private val mapper = jacksonObjectMapper()

    init {
        val temp = File(dir)
        if (!temp.exists()) {
            temp.mkdirs()
        }
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
    }

    /**
     * Save preprocess result
     *
     * @param results
     */
    fun saveObjectToJson(results: Any) {
        try {
            mapper.writeValue(File("$dir$fileName.json"), results)
        } catch (e: IOException) {
            println(e.printStackTrace())
            throw RuntimeException("Error while saving result to preprocessing file")
        }
    }

    fun retrieveObjectFromJson(): PreprocessStorage {
        try {
            return mapper.readValue(File("$dir$fileName.json"), PreprocessStorage::class.java)
        } catch (e: Exception) {
            println(e.printStackTrace())
            throw RuntimeException("Error while reading preprocess csv store")
        }
    }
}