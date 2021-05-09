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

package io.github.phantran.persistence

import io.github.phantran.engine.preprocessing.PreprocessClassResult

data class TestsCutMapping(
    override var entry: MutableMap<String, String> =
        mutableMapOf(
            "testClass" to "", "classesName" to ""
        ),
) : MoCoModel() {

    override val sourceName = "TestsCutMapping"

    fun getRecordedMapping(classes: List<String>): String {
        // This method return list of test classes that test the query classes
        val queryRes: MutableList<MoCoModel> = mutableListOf()
        classes.map { queryRes.addAll(this.getData("classesName LIKE '%${it}%'")) }
        val res: MutableSet<String> = mutableSetOf()
        for (i in queryRes.indices) {
            queryRes[i].entry["testClass"]?.let { res.addAll(it.split(",")) }
        }
        return res.joinToString(",")
    }

    fun saveMappingInfo(data: List<PreprocessClassResult>) {
        val temp: MutableMap<String, MutableSet<String>> = mutableMapOf()
        for (item in data) {
            for (test in item.testClasses) {
                if (!temp.keys.contains(test)) {
                    temp[test] = mutableSetOf(item.classUnderTestName)
                } else {
                    temp[test]?.add(item.classUnderTestName)
                }
            }
        }
        saveMultipleEntries(sourceName, temp.map {
            mutableMapOf( "testClass" to it.key, "classesName" to it.value.joinToString(","))
        })
    }

    companion object {
        const val schema: String =
            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                    "testClass VARCHAR(255)," +
                    "classesName TEXT," +
                    "createdAt timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE KEY uniqueClassMapping (testClass)"

    }
}