package io.moco.engine

data class Configuration (
    val buildRoot: String,
    val codeRoot: String,
    val testRoot: String,
    val excludedClasses: String,
    val classPath: List<String>,
    val jvm: String,
    val preprocessResultFileName: String,
    val mutationResultsFileName: String,
    val excludedMutationOperatorNames: String,
    val excludedTestClasses: String,
    val baseDir: String,
    val compileSourceRoots: List<String>?,
    val artifactId: String,
    val gitChangedClassesMode: Boolean = true
) {
    companion object {
        var currentConfig: Configuration? = null
    }
}