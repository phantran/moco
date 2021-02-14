package io.moco.engine.preprocessing

data class PreprocessStorage(
    val classRecord: List<PreprocessClassResult>
) {

    companion object {
        var storedStorage: PreprocessStorage? = null

        fun getStoredPreprocessStorage(buildRoot: String): PreprocessStorage {
            return if (storedStorage == null) {
                storedStorage = PreprocessConverter(buildRoot).retrievePreprocessResultFromJson()
                storedStorage as PreprocessStorage
            } else {
                storedStorage as PreprocessStorage
            }
        }
    }
}