package io.moco.engine.preprocessing

import com.fasterxml.jackson.databind.SerializationFeature
import java.io.*
import java.io.File

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Preprocess Converter
 * This class is responsible for exporting the map of class names to test class names to an JSON file
 * The exported JSON file will be read in each execution to retrieve this mapping information
 * It is also responsible for importing json file to create object of the PreprocessStorage class
 * @constructor Create empty Mapping exporter
 *
 * @param dir
 */
class PreprocessConverter(dir: String) {

    private var storePath: File? = null
    private val mapper = jacksonObjectMapper()

    init {
        val temp = File("$dir/moco/preprocess/")
        if (!temp.exists()) {
            temp.mkdirs()
        }
        this.storePath = File("$dir/moco/preprocess/coverage.json")
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
    }

    /**
     * Save preprocess result
     *
     * @param results
     */
    fun saveObjectToJson(results: Any) {
        try {
            mapper.writeValue(this.storePath, results)
        } catch (e: IOException) {
            println(e.printStackTrace())
            throw RuntimeException("Error while saving result to preprocessing file")
        }
    }

    fun retrieveObjectFromJson(): PreprocessStorage {
        try {
            return mapper.readValue(storePath, PreprocessStorage::class.java)
        } catch (e: Exception) {
            println(e.printStackTrace())
            throw RuntimeException("Error while reading preprocess csv store")
        }
    }
}