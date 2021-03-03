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


data class PersistentMutationResult(
    override var entry: MutableMap<String, String> =
        mutableMapOf(
            "commit_id" to "", "file_name" to "", "class_name" to "", "method_name" to "",
            "line_of_code" to "", "instruction_indices" to "", "mutator_id" to "",
            "mutation_description" to "", "operator_name" to "", "test_status" to "",
        ),
) : MoCoModel() {

    override val sourceName = "PersistentMutationResult"

    companion object {
        const val schema: String =
            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
            "commit_id VARCHAR(255)," +
            "file_name VARCHAR(255)," +
            "class_name VARCHAR(255)," +
            "method_name VARCHAR(255)," +
            "line_of_code INT(8) UNSIGNED NOT NULL," +
            "instruction_indices VARCHAR(255)," +
            "mutator_id VARCHAR(255)," +
            "mutation_description VARCHAR(255)," +
            "operator_name VARCHAR(255)," +
            "test_status VARCHAR(255)," +
            "created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "UNIQUE KEY unique_mutation (class_name, line_of_code, instruction_indices, mutator_id, commit_id)"
    }
}
