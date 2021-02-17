package io.moco.engine.preprocessing

import io.moco.engine.Configuration
import io.moco.utils.JsonConverter

data class PreprocessStorage(
    val classRecord: List<PreprocessClassResult>
) {

    companion object {
        var storedStorage: PreprocessStorage? = null

        fun getStoredPreprocessStorage(buildRoot: String): PreprocessStorage {
            return if (storedStorage == null) {
                storedStorage = JsonConverter("$buildRoot/moco/preprocess/",
                    Configuration.preprocessFilename
                ).retrieveObjectFromJson()
                storedStorage as PreprocessStorage
            } else {
                storedStorage as PreprocessStorage
            }
        }
    }
}