package io.moco.engine.preprocessing

import io.moco.engine.tracker.Block

/**
 * Preprocess result of a class under test
 * An instant of this class contains information that is collected during preprocessing of codebase of a specific class
 * Collected information are mapping of test classes and class under test and the information of blocks
 * Block information is needed in mutation process since finally block are not considered for mutation
 * @property testClassName
 * @property classUnderTestName
 * @property blockLists
 * @constructor Create empty Preprocess result
 */
data class PreprocessClassResult(
    val classUnderTestName: String,
    val testClasses: MutableList<String>,
    val blockLists: MutableList<Block>?,
) {
}