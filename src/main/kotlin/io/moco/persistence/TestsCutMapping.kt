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
            "className" to "", "testClass" to "", "coveredLines" to "", "commitID" to ""
        ),
) : MoCoModel() {

    override val sourceName = "TestsCutMapping"

    fun getRecordedMapping(classes: List<String>, commitID: String): String {
        // This method query classes by their names and git commit  id and
        // return list of corresponding tests
        val queryRes = this.getData(
            "commitID = \'$commitID\' AND className IN (\'${classes.joinToString("\',\'")}\')")
        var res = ""
        for (item in queryRes) {
            res += item.entry["testClass"]
        }
        return res
    }

    fun saveMappingInfo(data: List<PreprocessClassResult>, commitID: String) {
        val entries: List<MutableMap<String, String?>> = data.map {
            mutableMapOf(
                "className" to it.classUnderTestName,
                "testClass" to it.testClasses.joinToString(","),
                "commitID" to commitID
            )
        }
        saveMultipleEntries(sourceName, entries)
    }

    companion object {
        const val schema: String =
            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                    "className VARCHAR(255)," +
                    "testClass VARCHAR(255)," +
                    "commitID VARCHAR(255)," +
                    "createdAt timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }
}