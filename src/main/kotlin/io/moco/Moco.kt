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
 * Goal which touches a timestamp file.
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
     * Excluded source classes
     */
    @Parameter(defaultValue = "", property = "excludedSourceClasses", required = false)
    private val excludedSourceClasses: String = ""

    /**
     * Excluded tests classes, comma separated string
     */
    @Parameter(defaultValue = "", property = "excludedTestClasses", required = false)
    private val excludedTestClasses: String = ""

    /**
     * Excluded mutation operators, comma separated string
     */
    @Parameter(defaultValue = "", property = "excludedMutationOperatorNames", required = false)
    private val excludedMutationOperatorNames: String = ""

    /**
     * Set to true to tell MoCo to only generate mutation for changed classes based on git commit info
     */
    @Parameter(defaultValue = "true", property = "gitChangedClassesMode", required = false)
    private val gitChangedClassesMode: Boolean = true

    @Throws(MojoExecutionException::class)
    override fun execute() {
        try {
            val buildRoot =
                project?.build?.directory.toString()
            val codeRoot =
                project?.build?.outputDirectory.toString()
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
                classPath,
                jvm,
                preprocessResultFileName,
                mutationResultsFileName,
                excludedMutationOperatorNames,
                excludedTestClasses,
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