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

data class ProgressClassTest(
    override var entry: MutableMap<String, String> =
        mutableMapOf(
            "class_name" to "", "commit_id" to "",  "covered_operators" to "",
            "total_mutants" to "", "killed_mutants" to "",
        ),
) : MoCoModel() {

    override val sourceName = "ProgressClassTest"

    fun saveProgress(data: MutationStorage, commitID: String, configuredOperators: String) {
        val entries: MutableSet<MutableMap<String, String?>> = mutableSetOf()
        for ((key, value) in data.entries) {
            for (item in value) {
                entries.add(
                    mutableMapOf(
                        "class_name" to key,
                        "commit_id" to commitID,
                        "covered_operators" to configuredOperators,
                        "total_mutants" to value.size.toString(),
                        "killed_mutants" to value.count { it["result"] == "killed" }.toString(),
                    )
                )
            }
        }
        saveMultipleEntries(sourceName, entries.toList())
    }

    companion object {
         const val schema: String =
            "id INT NOT NULL AUTO_INCREMENT," +
            "class_name VARCHAR(255)," +
            "commit_id VARCHAR(255)," +
            "covered_operators VARCHAR(255)," +
            "total_mutants MEDIUMINT(8) UNSIGNED NOT NULL," +
            "killed_mutants MEDIUMINT(8) UNSIGNED NOT NULL," +
            "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "UNIQUE KEY unique_class_progress (class_name, covered_operators)"
    }
}