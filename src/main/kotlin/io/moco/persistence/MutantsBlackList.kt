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

import io.moco.engine.mutation.Mutation


data class MutantsBlackList(
    override var entry: MutableMap<String, String> =
        mutableMapOf(
            "class_name" to "", "line_of_code" to "",
            "instruction_indices" to "", "mutator_id" to ""
        ),
) : MoCoModel() {

    override val sourceName = "MutantsBlackList"


    fun saveErrorMutants(data: MutationStorage) {
        val entries: MutableSet<MutableMap<String, String?>> = mutableSetOf()
        for ((key, value) in data.entries) {
            for (item in value) {
                if (item["result"] as String == "run_error") {
                    val mutationDetails = item["mutationDetails"] as Mutation
                    val mutationID = mutationDetails.mutationID
                    entries.add(
                        mutableMapOf(
                            "class_name" to key,
                            "line_of_code" to mutationDetails.lineOfCode.toString(),
                            "instruction_indices" to mutationID.instructionIndices!!.joinToString(","),
                            "mutator_id" to mutationID.mutatorUniqueID,
                        )
                    )
                }
            }
        }
        saveMultipleEntries(sourceName, entries.toList())
    }

    companion object {
        const val schema: String =
            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
            "class_name VARCHAR(255)," +
            "line_of_code INT(8) UNSIGNED NOT NULL," +
            "instruction_indices VARCHAR(255)," +
            "mutator_id VARCHAR(255)," +
            "created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }
}
