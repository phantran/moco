package io.moco

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File


/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "touch", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
class Moco : AbstractMojo() {
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
        log.info( "Hello, world." )
//        val f = outputDirectory
//        if (!f!!.exists()) {
//            f.mkdirs()
//        }
//        val touch = File(f, "touch.txt")
//        var w: FileWriter? = null
//        try {
//            w = FileWriter(touch)
//            w.write("touch.txt")
//        } catch (e: IOException) {
//            throw MojoExecutionException("Error creating file $touch", e)
//        } finally {
//            if (w != null) {
//                try {
//                    w.close()
//                } catch (e: IOException) {
//                    // ignore
//                }
//            }
//        }
    }
}