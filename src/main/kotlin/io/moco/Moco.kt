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
     * Location of the file.
     */
    @Parameter(defaultValue = "\${project.build.directory}", property = "outputDir", required = true)
    private val outputDirectory: File? = null

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


    @Throws(MojoExecutionException::class)
    override fun execute() {
        try {
            println(project?.basedir)
            println(project?.compileSourceRoots)
            println(project?.groupId)
            println(project?.artifactId)


            val buildRoot =
                project?.build?.directory.toString()
            val codeRoot =
                project?.build?.outputDirectory.toString()
            val testRoot =
                project?.build?.testOutputDirectory.toString()
            val runtimeClassPath = project?.runtimeClasspathElements
            val classPath =
                runtimeClassPath
                    ?: System.getProperty(
                        "java.class.path"
                    ).split(File.pathSeparatorChar.toString())
            val jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"

            Configuration.setConfiguration(
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
                project?.artifactId,

            )

            MocoEntryPoint().execute()

        } catch (e: Exception) {
            log.info(e.printStackTrace().toString())
        }


    }
}