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

package io.moco

import io.moco.engine.Configuration
import io.moco.engine.MoCoEntryPoint
import io.moco.engine.operator.Operator
import io.moco.persistence.H2Database
import io.moco.utils.MoCoLogger
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import java.io.File
import org.apache.maven.project.MavenProject
import org.h2.jdbc.JdbcSQLNonTransientConnectionException
import java.lang.Exception

/**
 * Goal which perform mutation tests, collect mutation information and store mutation information into JSON file
 */
@Mojo(
    name = "moco",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE
)
class MoCo : AbstractMojo() {

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    var project: MavenProject? = null


    /**
     * Preprocess storage file name
     */
    @Parameter(defaultValue = "preprocess", property = "preprocessResultsFolder", required = false)
    private val preprocessResultsFolder: String = "preprocess"

    /**
     * Mutation result storage file name
     */
    @Parameter(defaultValue = "mutation", property = "mutationResultsFolder", required = false)
    private val mutationResultsFolder: String = "mutation"

    /**
     * Excluded source classes, comma separated string, specify as class name with "/", example: io/moco/Example
     */
    @Parameter(defaultValue = "", property = "excludedSourceClasses", required = false)
    private val excludedSourceClasses: String = ""

    /**
     * Excluded source folder, comma separated string
     */
    @Parameter(defaultValue = "", property = "excludedSourceFolder", required = false)
    private val excludedSourceFolders: String = ""

    /**
     * Set to false to display succinct console messages during MoCo execution
     */
    @Parameter(defaultValue = "moco", property = "mocoRoot", required = false)
    private val mocoRoot: String = "moco"

    /**
     * Because MoCo modifies byte code of class under test during runtime for instrumentation purpose,
     * There might be cases a test framework can detect infinite loop and exit early, but it cannot detect such
     * cases when MoCo insert some instrumentation instructions to the byte code. MoCo cannot escape the loop and
     * finish its execution. For such cases, please turn on debugging to check which one of your test cases cause
     * an infinite loop or configure preprocessTestTimeout parameter (unit milliseconds), by default preprocessTestTimeout parameter is
     * not set. Since it assumes all test cases can function properly before MoCo performing mutation testing.
     *
     * Value of 0 means no time out will be taken into consideration
     * This value of test time out is only used in case timeout cannot be automatically calculated by using
     * recorded execution time of that test.
     */
    @Parameter(defaultValue = "-1", property = "preprocessTestTimeout", required = false)
    private val preprocessTestTimeout: String = "-1"

    /**
     * Excluded tests classes, comma separated string, specify as class name with "/", example: io/moco/TestExample
     */
    @Parameter(defaultValue = "", property = "excludedTestClasses", required = false)
    private val excludedTestClasses: String = ""

    /**
     * Excluded test folder, comma separated string
     */
    @Parameter(defaultValue = "", property = "excludedTestFolder", required = false)
    private val excludedTestFolders: String = ""

    /**
     * Excluded mutation operators, comma separated string
     */
    @Parameter(defaultValue = "", property = "excludedMuOpNames", required = false)
    private val excludedMuOpNames: String = ""

    /**
     * Set to true to tell MoCo to only generate mutation only for changed classes based on git commit information
     */
    @Parameter(defaultValue = "true", property = "gitMode", required = false)
    private val gitMode: Boolean = true

    /**
     * Set to true to tell MoCo to only generate mutation only for changed classes based on git commit information
     */
    @Parameter(defaultValue = "5", property = "mutationPerClass", required = false)
    private val mutationPerClass: Int = 5

    /**
     * Set to true to tell MoCo to only generate mutation only for changed classes based on git commit information
     */
    @Parameter(defaultValue = "false", property = "debugEnabled", required = false)
    private val debugEnabled: Boolean = false

    /**
     * Set to false to display succinct console messages during MoCo execution
     */
    @Parameter(defaultValue = "true", property = "verbose", required = false)
    private val verbose: Boolean = true

    /**
     * Number of max threads to use by the main process of MoCo
     */
    @Parameter(defaultValue = "2", property = "numberOfThreads", required = false)
    private val numberOfThreads: Int = 2

    /**
     * Set to true to calculate mutation score for each run
     */
    @Parameter(defaultValue = "false", property = "enableMetrics", required = false)
    private val enableMetrics: Boolean = false

    /**
     * Turn off to skip dumbing mutation test results to moco.json file at the end
     */
    @Parameter(defaultValue = "true", property = "useForCICD", required = false)
    private val useForCICD: Boolean = true


    @Parameter(defaultValue = "\${localRepository}", readonly = true, required = true)
    private val localRepository: ArtifactRepository? = null


    @Throws(MojoExecutionException::class)
    override fun execute() {
        try {
            val persistencePath = localRepository?.basedir + "/io/moco/persistence/moco"
            // Often named as "target" or "build" folder, contains compiled classes, JaCoCo report, MoCo report, etc...
            val buildRoot =
                project?.build?.directory.toString()
            // contains compiled source classes .class files
            val codeRoot =
                project?.build?.outputDirectory.toString()
            // contains compiled test classes .class files
            val testRoot =
                project?.build?.testOutputDirectory.toString()

            val runtimeCp = project?.runtimeClasspathElements
            val compileCp = project?.compileClasspathElements


            val jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
            val mocoBuildPath = "$buildRoot${File.separator}$mocoRoot"

            val fOpNames = Operator.supportedOperatorNames.filter { !excludedMuOpNames.contains(it) }

            val temp = System.getProperty("java.class.path").split(File.pathSeparator)
                .union(runtimeCp!!.toSet()).union(compileCp!!.toSet()).toList()
                .joinToString(separator = File.pathSeparator)
            val classPath = "$temp:$codeRoot:${testRoot}:${buildRoot}"

            val configuration = Configuration(
                buildRoot,
                codeRoot,
                testRoot,
                mocoBuildPath,
                excludedSourceClasses,
                excludedSourceFolders,
                excludedTestClasses,
                excludedTestFolders,
                classPath,
                jvm,
                preprocessResultsFolder,
                mutationResultsFolder,
                excludedMuOpNames,
                fOpNames,
                project?.basedir.toString(),
                project?.compileSourceRoots,
                project?.artifactId!!,
                gitMode,
                preprocessTestTimeout,
                mutationPerClass,
                debugEnabled,
                verbose,
                numberOfThreads,
                enableMetrics,
                useForCICD
            )

            Configuration.currentConfig = configuration
            MoCoLogger.useMvnLog(log)
            MoCoLogger.debugEnabled = Configuration.currentConfig!!.debugEnabled
            H2Database.initPool(
                url = "jdbc:h2:file:${persistencePath};mode=MySQL;",
                user = "moco",
                password = "moco",
            )
            H2Database().initDBTablesIfNotExists()
            MoCoEntryPoint(configuration).execute()
        } catch (e: Exception) {
            when (e) {
                is JdbcSQLNonTransientConnectionException -> log.error("Exit - MoCo Embedded database Error - ${e.message}")
                else -> log.error(e.message)

            }
        } finally {
            H2Database.shutDownDB()
        }
    }
}

