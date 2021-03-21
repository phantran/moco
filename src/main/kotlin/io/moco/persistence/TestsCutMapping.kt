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

import io.moco.engine.preprocessing.PreprocessClassResult

data class TestsCutMapping(
    override var entry: MutableMap<String, String> =
        mutableMapOf(
            "className" to "", "testClass" to ""
        ),
) : MoCoModel() {

    override val sourceName = "TestsCutMapping"

    fun getRecordedMapping(classes: List<String>): String {
        // This method query classes by their names and git commit  id and
        // return list of corresponding tests
        val queryRes = this.getData(
            "className IN (\'${classes.joinToString("\',\'")}\')")
        val temp: MutableSet<String> = mutableSetOf()
        for (i in queryRes.indices) {
            queryRes[i].entry["testClass"]?.let { temp.addAll(it.split(",")) }
        }
        return temp.joinToString(",")
    }

    fun saveMappingInfo(data: List<PreprocessClassResult>) {
        val entries: List<MutableMap<String, String?>> = data.map {
            mutableMapOf(
                "className" to it.classUnderTestName,
                "testClass" to it.testClasses.joinToString(","),
            )
        }
        saveMultipleEntries(sourceName, entries)
    }

    companion object {
        const val schema: String =
            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                    "className VARCHAR(255)," +
                    "testClass VARCHAR(255)," +
                    "createdAt timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }
}