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


data class ProjectTestHistory(
    override var entry: MutableMap<String, String> =
        mutableMapOf(
            "commit_id" to "", "branch" to "", "run_operators" to "",
            "run_coverage" to "", "accumulated_coverage" to "",
            "git_mode" to "", "timestamp" to ""
        ),
) : MoCoModel() {

    override val sourceName = "ProjectTestHistory"

    companion object {
        const val schema: String =
            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
            "commit_id VARCHAR(255)," +
            "branch VARCHAR(255)," +
            "run_operators VARCHAR(255)," +
            "run_coverage VARCHAR(255)," +
            "accumulated_coverage VARCHAR(255)," +
            "git_mode BOOL NOT NULL DEFAULT FALSE," +
            "timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "UNIQUE KEY unique_test_run (commit_id, run_operators, timestamp)"
    }
}
