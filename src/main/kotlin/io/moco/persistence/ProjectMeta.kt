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

import io.moco.engine.Configuration
import io.moco.utils.MoCoLogger

data class ProjectMeta(
    var meta: MutableMap<String, String> = mutableMapOf("latestStoredCommitID" to "", "latestStoredBranchName" to "")
) : MoCoModel() {

    val logger = MoCoLogger()
    override val persistenceMode = Configuration.currentConfig?.persistenceMode
    override val sourceName = "ProjectMeta"
    override var source = H2Database()

    init {
        try {
            getData()
        } catch (e: Exception) {
            logger.error("Error while getting $sourceName")
        }
    }

    override fun getData() {
        if (source.connection == null) {
            source = H2Database()
        }
        val res = source.fetch(sourceName)
        while (res?.next() == true) {
            meta[res.getString("meta_key")] = res.getString("meta_value")
        }
    }

    override fun save() {
        if (source.connection == null) {
            source = H2Database()
        }
        if (persistenceMode == "database") {
            meta.map {
                source.multipleInsertOrUpdateIfExist(sourceName, listOf(mapOf("meta_key" to it.key, "meta_value" to it.value)))
            }
        }
        source.closeConnection()
    }

    companion object {
        val schema: Map<String, String> = mapOf(
            "id" to "int NOT NULL AUTO_INCREMENT",
            "meta_key" to "varchar(255) PRIMARY KEY",
            "meta_value" to "varchar(255)"
        )
    }
}