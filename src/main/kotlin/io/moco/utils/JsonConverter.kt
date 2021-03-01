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

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.moco.engine.mutation.MutationStorage
import io.moco.engine.preprocessing.PreprocessClassResult
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
    private val logger = MoCoLogger()

    init {
        val temp = File(dir)
        if (!temp.exists()) {
            temp.mkdirs()
        }
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
    }

    fun removeJSONFileIfExists() {
        val temp = File("$dir$fileName.json")
        if (temp.exists()) {
            temp.delete()
            logger.debug("Existing preprocess JSON file was removed")
        }
    }

    /**
     * Save preprocess result
     *
     * @param results
     */
    fun savePreprocessToJson(results: PreprocessStorage) {
        try {
            val existingStorage = retrieveObjectFromJson()
            if (existingStorage == null) {
                mapper.writeValue(File("$dir$fileName.json"), results)
            } else {
                for ((k, v) in results.testsExecutionTime!!) {
                    existingStorage.testsExecutionTime!!.putIfAbsent(k, v)
                }

                for (item in results.classRecord) {

                    val foundIndex = existingStorage.classRecord.indexOfFirst { it.classUnderTestName == item.classUnderTestName }
                    if (foundIndex != -1) {
                        existingStorage.classRecord[foundIndex].testClasses.addAll(item.testClasses)
                        existingStorage.classRecord[foundIndex].coveredLines?.addAll(item.coveredLines!!)
                    } else {
                        existingStorage.classRecord.add(item)
                    }
                }
                mapper.writeValue(File("$dir$fileName.json"), existingStorage)
            }
        } catch (e: IOException) {
            logger.error(e.printStackTrace().toString())
            throw RuntimeException("Error while saving preprocessing results to csv file")
        }
    }

    /**
     * Save mutation results
     *
     * @param results
     */
    fun saveMutationResultsToJson(results: MutationStorage) {
        try {
            mapper.writeValue(File("$dir$fileName.json"), results)
        } catch (e: IOException) {
            logger.error(e.printStackTrace().toString())
            throw RuntimeException("Error while saving mutation results to csv file")
        }
    }


    fun retrieveObjectFromJson(): PreprocessStorage? {
        val temp = File("$dir$fileName.json")
        if (!temp.exists()) {
            return null
        }
        return try {
            mapper.readValue(temp, PreprocessStorage::class.java)
        } catch (e: Exception) {
            logger.error("Error while reading JSON file")
            null
        }
    }
}