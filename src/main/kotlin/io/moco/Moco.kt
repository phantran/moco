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
import io.moco.engine.MocoEntryPoint
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import java.io.File
import org.apache.maven.project.MavenProject
import java.lang.Exception


/**
 * Goal which perform mutation tests, collect mutation information and store mutation information into JSON file
 */
@Mojo(
    name = "moco",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE
)
class Moco : AbstractMojo() {

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    var project: MavenProject? = null


    /**
     * Preprocess storage file name
     */
    @Parameter(defaultValue = "preprocess", property = "preprocessFilename", required = false)
    private val preprocessResultFileName: String = "preprocess"

    /**
     * Mutation result storage file name
     */
    @Parameter(defaultValue = "moco", property = "mutationResultsFilename", required = false)
    private val mutationResultsFileName: String = "moco"

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
    @Parameter(defaultValue = "", property = "excludedMutationOperatorNames", required = false)
    private val excludedMutationOperatorNames: String = ""

    /**
     * Set to true to tell MoCo to only generate mutation only for changed classes based on git commit information
     */
    @Parameter(defaultValue = "true", property = "gitChangedClassesMode", required = false)
    private val gitChangedClassesMode: Boolean = true

    @Throws(MojoExecutionException::class)
    override fun execute() {
        try {
            // Often named as "target" or "build" folder, contains compiled classes, JaCoCo report, MoCo report, etc...
            val buildRoot =
                project?.build?.directory.toString()
            // contains compiled source classes .class files
            val codeRoot =
                project?.build?.outputDirectory.toString()
            // contains compiled test classes .class files
            val testRoot =
                project?.build?.testOutputDirectory.toString()

            val runtimeClassPath = project?.runtimeClasspathElements
            val classPath = runtimeClassPath?: System.getProperty("java.class.path").
                                                                split(File.pathSeparatorChar.toString())
            val jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"

            val configuration = Configuration(
                buildRoot,
                codeRoot,
                testRoot,
                excludedSourceClasses,
                excludedSourceFolders,
                excludedTestClasses,
                excludedTestFolders,
                classPath,
                jvm,
                preprocessResultFileName,
                mutationResultsFileName,
                excludedMutationOperatorNames,
                project?.basedir.toString(),
                project?.compileSourceRoots,
                project?.artifactId!!,
                gitChangedClassesMode
            )

            Configuration.currentConfig = configuration
            MocoEntryPoint(configuration).execute()

        } catch (e: Exception) {
            log.info(e.printStackTrace().toString())
        }
    }
}