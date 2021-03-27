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

package io.moco.persistence

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.moco.engine.preprocessing.PreprocessStorage
import io.moco.utils.MoCoLogger
import java.io.File
import java.io.IOException

/**
 * Json Source
 * This class is responsible for exporting the map of class names to test class names to an JSON file
 * The exported JSON file will be read in each execution to retrieve this mapping information
 * It is also responsible for importing json file to create object of the PreprocessStorage class
 * @constructor Create empty Mapping exporter
 *
 * @param folderPath
 * @param fileName
 */
class JsonSource(private val folderPath: String, private val fileName: String) : DataSource() {

    private val mapper = jacksonObjectMapper()
    private val logger = MoCoLogger()

    init {
        val temp = File(folderPath)
        if (!temp.exists()) {
            temp.mkdirs()
        }
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
    }

    fun removeJSONFileIfExists() {
        val temp = File("$folderPath${File.separator}$fileName.json")
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
            var existingStorage = getData(PreprocessStorage::class.java)
            if (existingStorage == null) {
                // No existing storage, just save the result
                mapper.writeValue(File("$folderPath${File.separator}$fileName.json"), results)
            } else {
                // Handle the case when a failed test made the preprocess process exited
                // We need to read the previous storage and append to it
                existingStorage = existingStorage as PreprocessStorage
                // First we update test execution time keys values
                // Next we append to the existing class records
                for (item in results.classRecord) {
                    val foundIndex =
                        existingStorage.classRecord.indexOfFirst { it.classUnderTestName == item.classUnderTestName }
                    if (foundIndex != -1) {
                        existingStorage.classRecord[foundIndex].testClasses.addAll(item.testClasses)
                        // Update line to tests mapping
                        val lineTestMapping = existingStorage.classRecord[foundIndex].coveredLines
                        item.coveredLines?.map {
                            if (lineTestMapping?.containsKey(it.key) == true) {
                                lineTestMapping[it.key]?.addAll(it.value)
                            } else {
                                lineTestMapping?.set(it.key, it.value)
                            }
                        }
                    } else {
                        existingStorage.classRecord.add(item)
                    }
                }
                mapper.writeValue(File("$folderPath${File.separator}$fileName.json"), existingStorage)
            }
        } catch (e: IOException) {
            logger.error(e.printStackTrace().toString())
            throw RuntimeException("Error while saving preprocessing results to csv file")
        }
    }

    /**
     * Store data that need to be persisted
     *
     * @param data
     */
    override fun save(data: Any) {
        try {
            mapper.writeValue(File("$folderPath${File.separator}$fileName.json"), data)
        } catch (e: IOException) {
            logger.error(e.printStackTrace().toString())
            throw RuntimeException("Error while saving mutation results to csv file")
        }
    }


    override fun getData(cls: Class<*>): Any? {
        val temp = File("$folderPath${File.separator}$fileName.json")
        if (!temp.exists()) {
            return null
        }
        return try {
            mapper.readValue(temp, cls)
        } catch (e: Exception) {
            logger.error("Error while reading JSON file")
            logger.error(e.printStackTrace().toString())
            null
        }
    }
}