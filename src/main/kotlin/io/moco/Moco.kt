package io.moco

import io.moco.engine.MocoEntryPoint
import io.moco.engine.preprocessing.PreprocessConverter
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
     * The greeting to display.
     */
    @Parameter(property = "sayhi.greeting", defaultValue = "Hello World!")
    private val greeting: String? = null

    @Throws(MojoExecutionException::class)
    override fun execute() {
        try {
            val buildRoot =
                project?.build?.directory.toString()
            val codeRoot =
                project?.build?.outputDirectory.toString()
            val testRoot =
                project?.build?.testOutputDirectory.toString()
            val runtimeClassPath =
                project?.runtimeClasspathElements
            val compileClassPath =
                project?.compileClasspathElements

            if (runtimeClassPath != null) {
                if (compileClassPath != null) {
                    MocoEntryPoint(
                        codeRoot, testRoot, "", buildRoot,
                        runtimeClassPath, compileClassPath
                    ).execute()
                }
            }
            val abc = PreprocessConverter(buildRoot).retrievePreprocessResultFromJson()
        } catch (e: Exception) {
            log.info(e.printStackTrace().toString())
        }


    }
}