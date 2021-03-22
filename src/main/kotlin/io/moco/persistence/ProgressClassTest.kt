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
            "className" to "", "coveredOperators" to "",
            "totalMutants" to "", "killedMutants" to "",
        ),
) : MoCoModel() {

    override val sourceName = "ProgressClassTest"

    fun saveProgress(data: MutationStorage, configuredOperators: String) {
        val entries: MutableSet<MutableMap<String, String?>> = mutableSetOf()
        for ((key, value) in data.entries) {
            entries.add(
                mutableMapOf(
                    "className" to key,
                    "coveredOperators" to configuredOperators,
                    "totalMutants" to value.count { it["result"] != "run_error" }.toString(),
                    "killedMutants" to value.count { it["result"] == "killed" }.toString(),
                )
            )
        }
        saveMultipleEntries(sourceName, entries.toList())
    }

    companion object {
        const val schema: String =
            "id INT NOT NULL AUTO_INCREMENT," +
                    "className VARCHAR(255)," +
                    "coveredOperators VARCHAR(255)," +
                    "totalMutants MEDIUMINT(8) UNSIGNED NOT NULL," +
                    "killedMutants MEDIUMINT(8) UNSIGNED NOT NULL," +
                    "createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE KEY uniqueClassProgress (className, coveredOperators)"
    }
}