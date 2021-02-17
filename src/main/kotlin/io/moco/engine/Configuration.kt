package io.moco.engine

class Configuration {
    companion object {
        lateinit var buildRoot: String
        lateinit var codeRoot: String
        lateinit var testRoot: String
        lateinit var excludedClasses: String
        lateinit var classPath: List<String>
        lateinit var jvm: String
        lateinit var preprocessFilename: String
        lateinit var mutationResultsFilename: String
        lateinit var excludedMutationOperatorNames: String
        lateinit var excludedTestClasses: String

        fun setConfiguration(
            buildRoot: String,
            codeRoot: String,
            testRoot: String,
            excludedClasses: String,
            classPath: List<String>,
            jvm: String,
            preprocessFilename: String,
            mutationResultsFilename: String,
            excludedMutationOperatorNames: String,
            excludedTestClasses: String
        ) {
            Configuration.buildRoot = buildRoot
            Configuration.codeRoot = codeRoot
            Configuration.testRoot = testRoot
            Configuration.excludedClasses = excludedClasses
            Configuration.classPath = classPath
            Configuration.jvm = jvm
            Configuration.preprocessFilename = preprocessFilename
            Configuration.mutationResultsFilename = mutationResultsFilename
            Configuration.excludedMutationOperatorNames = excludedMutationOperatorNames
            Configuration.excludedTestClasses = excludedTestClasses
        }
    }
}