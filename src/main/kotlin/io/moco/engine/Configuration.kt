package io.moco.engine

import org.apache.maven.plugin.logging.Log

data class Configuration(
    val buildRoot: String,
    val codeRoot: String,
    val testRoot: String,
    val excludedSourceClasses : String,
    val excludedSourceFolders : String,
    val excludedTestClasses : String,
    val excludedTestFolders : String,
    val classPath: List<String>,
    val jvm: String,
    val preprocessResultFileName: String,
    val mutationResultsFileName: String,
    val excludedMutationOperatorNames: String,
    val baseDir: String,
    val compileSourceRoots: List<String>?,
    val artifactId: String,
    val gitChangedClassesMode: Boolean,
    val testTimeOut: String
) {
    companion object {
        var currentConfig: Configuration? = null
    }

    fun getPreprocessProcessArgs(): MutableList<String> {
        return mutableListOf(buildRoot, codeRoot, testRoot, excludedSourceClasses, excludedSourceFolders,
            excludedTestClasses, excludedTestFolders, preprocessResultFileName, testTimeOut
        )
    }
}