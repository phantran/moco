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


data class ProjectMeta(
    override var entry: MutableMap<String, String> = mutableMapOf("meta_key" to "", "meta_value" to ""),
    var meta: MutableMap<String, String> = mutableMapOf(
        "latestStoredCommitID" to "",
        "latestStoredBranchName" to "",
        "sourceBuildFolder" to "",
        "testBuildFolder" to "",
        "runOperators" to "",)
) : MoCoModel() {

    override val sourceName = "ProjectMeta"

    init {
        getMetaData()
    }

    private fun getMetaData() {
        val retrieved = this.getData("")
        for (item in retrieved) {
            val metaKey: String? = item.entry["meta_key"]
            if (meta.keys.contains(metaKey)) {
                meta[metaKey!!] = item.entry["meta_value"]!!
            }
        }
    }

    fun saveMetaData() {
        val entries: MutableList<MutableMap<String, String?>> = mutableListOf()
        for ((k,v) in meta) {
            entries.add(mutableMapOf("meta_key" to k, "meta_value" to v))
        }
        saveMultipleEntries(sourceName, entries)
    }

    companion object {
        const val schema: String =
            "id INT NOT NULL AUTO_INCREMENT," +
            "meta_key VARCHAR(255) PRIMARY KEY," +
            "meta_value VARCHAR(255)"
    }
}
