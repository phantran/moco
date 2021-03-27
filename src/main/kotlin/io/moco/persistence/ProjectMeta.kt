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

import kotlin.jvm.Throws


data class ProjectMeta(
    override var entry: MutableMap<String, String> = mutableMapOf("metaKey" to "", "metaValue" to ""),
    var meta: MutableMap<String, String> = mutableMapOf(
        "lastRunID" to "",
        "lastMavenSessionID" to "",
        "storedHeadCommit" to "",
        "storedPreviousHeadCommit" to "",
        "latestStoredBranchName" to "",
        "sourceBuildFolder" to "",
        "artifactId" to "",
        "groupId" to "",
        "mocoVersion" to "",
        "testBuildFolder" to "",
        "runOperators" to "",)
) : MoCoModel() {

    override val sourceName = "ProjectMeta"

    init {
        getMetaData()
    }

    @Throws(Exception::class)
    private fun getMetaData() {
        val retrieved = this.getData("")
        for (item in retrieved) {
            val metaKey: String? = item.entry["metaKey"]
            if (meta.keys.contains(metaKey)) {
                meta[metaKey!!] = item.entry["metaValue"]!!
            }
        }
    }

    fun saveMetaData() {
        val entries: MutableList<MutableMap<String, String?>> = mutableListOf()
        for ((k,v) in meta) {
            entries.add(mutableMapOf("metaKey" to k, "metaValue" to v))
        }
        saveMultipleEntries(sourceName, entries)
    }

    companion object {
        const val schema: String =
            "id INT NOT NULL AUTO_INCREMENT," +
            "metaKey VARCHAR(255) PRIMARY KEY," +
            "metaValue VARCHAR(255)"
    }
}
