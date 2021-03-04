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


package io.moco.engine.preprocessing

import io.moco.engine.Configuration
import io.moco.persistence.JsonSource
import java.io.File

/**
 * Preprocess storage
 *
 * @property classRecord
 * @constructor Create empty Preprocess storage
 */
data class PreprocessStorage(
    val classRecord: MutableList<PreprocessClassResult>,
    val testsExecutionTime: MutableMap<String, Long>?,
    val previousRemainingTests: List<String?>,
    val errorTests: MutableList<String?>,
) {

    companion object {
        private var storedStorage: PreprocessStorage? = null

        fun getPreprocessStorage(mocoBuildPath: String): PreprocessStorage? {
            return if (storedStorage == null) {
                storedStorage = JsonSource(
                    "$mocoBuildPath${File.separator}${Configuration.currentConfig?.preprocessResultsFolder}",
                    "preprocess"
                ).getData(PreprocessStorage::class.java) as PreprocessStorage
                if (storedStorage != null) storedStorage else null
            } else {
                storedStorage as PreprocessStorage
            }
        }
    }
}