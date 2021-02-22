package io.moco.engine

import java.io.File

class Configuration {
    companion object {
        lateinit var buildRoot: String
        lateinit var codeRoot: String
        lateinit var testRoot: String
        var excludedClasses: String? = ""
        lateinit var classPath: List<String>
        lateinit var jvm: String
        var preprocessResultFileName: String? = ""
        var mutationResultsFileName: String? = ""
        var excludedMutationOperatorNames: String? = ""
        private var excludedTestClasses: String? = ""
        var baseDir: String? = ""
        var compileSourceRoots: List<String>? = mutableListOf()
        var artifactId: String? = ""

        fun setConfiguration(
            buildRoot: String,
            codeRoot: String,
            testRoot: String,
            excludedClasses: String?,
            classPath: List<String>,
            jvm: String,
            preprocessResultFileName: String?,
            mutationResultsFileName: String?,
            excludedMutationOperatorNames: String?,
            excludedTestClasses: String?,
            baseDir: String?,
            compileSourceRoots: List<String>?,
            artifactId: String?
            ) {
            Configuration.buildRoot = buildRoot
            Configuration.codeRoot = codeRoot
            Configuration.testRoot = testRoot
            Configuration.excludedClasses = excludedClasses
            Configuration.classPath = classPath
            Configuration.jvm = jvm
            Configuration.preprocessResultFileName = preprocessResultFileName
            Configuration.mutationResultsFileName = mutationResultsFileName
            Configuration.excludedMutationOperatorNames = excludedMutationOperatorNames
            Configuration.excludedTestClasses = excludedTestClasses
            Configuration.baseDir = baseDir
            Configuration.compileSourceRoots = compileSourceRoots
            Configuration.artifactId = artifactId
        }
    }
}