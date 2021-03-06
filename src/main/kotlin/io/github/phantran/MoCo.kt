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

package io.github.phantran

import io.github.phantran.engine.Configuration
import io.github.phantran.engine.MoCoEntryPoint
import io.github.phantran.engine.operator.Operator
import io.github.phantran.persistence.H2Database
import io.github.phantran.utils.MoCoLogger
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import java.io.File
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.execution.MavenSession
import java.nio.file.Paths

/**
 * Goal which perform mutation tests, collect mutation information and store mutation information into JSON file
 */
@Mojo(
    name = "moco",
    defaultPhase = LifecyclePhase.VERIFY,
    requiresDependencyResolution = ResolutionScope.TEST,
    threadSafe = true
)
class MoCo : AbstractMojo() {

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    var project: MavenProject? = null

    /**
     * Path to the directory of compiled source code
     */
    @Parameter(defaultValue = "", property = "codeRootDir", required = false)
    private val codeRootDir: String = ""

    /**
     * Path to the directory of compiled test code
     */
    @Parameter(defaultValue = "", property = "testRootDir", required = false)
    private val testRootDir: String = ""

    /**
     * Preprocess storage folder name (The folder that contains preprocess.json file)
     */
    @Parameter(defaultValue = "preprocess", property = "preprocessResultsFolder", required = false)
    private val preprocessResultsFolder: String = "preprocess"


    /**
     * Mutation result storage folder name (The folder that contains moco.json file)
     */
    @Parameter(defaultValue = "mutation", property = "mutationResultsFolder", required = false)
    private val mutationResultsFolder: String = "mutation"

    /**
     * Excluded source classes, comma separated string, specify as class name with "/", Example: org/example/Example
     */
    @Parameter(defaultValue = "", property = "excludedSourceClasses", required = false)
    private val excludedSourceClasses: String = ""

    /**
     * Excluded source folder (built class), comma separated string. Example: org/example/abc,org/example/xyz
     */
    @Parameter(defaultValue = "", property = "excludedSourceFolder", required = false)
    private val excludedSourceFolders: String = ""

    /**
     * MoCo build folder name that contains all generated sources by MoCo, it's better to keep this as default
     */
    @Parameter(defaultValue = "moco", property = "mocoRoot", required = false)
    private val mocoRoot: String = "moco"

    /**
     * Time out for a test in the preprocessing phase of MoCo (ms)
     */
    @Parameter(defaultValue = "-1", property = "preprocessTestTimeout", required = false)
    private val preprocessTestTimeout: String = "-1"

    /**
     * Excluded tests classes, comma separated string, specify as class name with "/", example: org/example/TestExample
     */
    @Parameter(defaultValue = "", property = "excludedTestClasses", required = false)
    private val excludedTestClasses: String = ""

    /**
     * Excluded test folder, comma separated string, example: org/example/abc,org/example/xyz
     */
    @Parameter(defaultValue = "", property = "excludedTestFolder", required = false)
    private val excludedTestFolders: String = ""

    /**
     * Excluded mutation operators, comma separated string, for example: AOR,ROR
     */
    @Parameter(defaultValue = "", property = "excludedMuOpNames", required = false)
    private val excludedMuOpNames: String = ""

    /**
     * Set to true to tell MoCo to only generate mutation only for changed classes based on git commit information
     */
    @Parameter(defaultValue = "true", property = "gitMode", required = false)
    private val gitMode: Boolean = true

    /**
     * Turn off debug messages to console
     */
    @Parameter(defaultValue = "false", property = "debugEnabled", required = false)
    private val debugEnabled: Boolean = false

    /**
     * Set to false to display succinct console messages during MoCo execution
     */
    @Parameter(defaultValue = "false", property = "verbose", required = false)
    private val verbose: Boolean = false

    /**
     * Maximum number of threads to use by the main process of MoCo
     */
    @Parameter(defaultValue = "2", property = "numberOfThreads", required = false)
    private val numberOfThreads: Int = 2

    /**
     * Set to true to calculate mutation score for each run (this feature is not stable yet)
     */
    @Parameter(defaultValue = "true", property = "enableMetrics", required = false)
    private val enableMetrics: Boolean = true

    /**
     * Turn off to skip storing mutation test results to moco.json file at the end of MoCo execution
     */
    @Parameter(defaultValue = "true", property = "useForCICD", required = false)
    private val useForCICD: Boolean = true

    /**
     * Set this parameter to true to limit the number mutations of each mutation operator on each line to just 1
     * Set this parameter to false to tell MoCo to generate all possible mutations that it could collect on a line of code
     */
    @Parameter(defaultValue = "true", property = "filterMutants", required = false)
    private val filterMutants: Boolean = true

    /**
     * Set to true to skip MoCo execution
     */
    @Parameter(defaultValue = "false", property = "skip", required = false)
    private val skip: Boolean = false

    @Parameter(defaultValue = "\${mojoExecution}", readonly = true, required = true)
    private val mojo: MojoExecution? = null

    @Parameter(defaultValue = "\${localRepository}", readonly = true, required = true)
    private val localRepository: ArtifactRepository? = null

    @Parameter(defaultValue = "\${session}", readonly = true)
    private val session: MavenSession? = null


    @Throws(MojoExecutionException::class)
    override fun execute() {
        var dbON = false
        try {
            if (!skip) {
                if (project != null && project!!.build != null) {
                    // Skip if MoCo dependency is not in pom.xml
                    if (project?.dependencies?.any {
                            it.artifactId == mojo?.artifactId && it.groupId == mojo?.groupId
                        } == false) {
                        log.info("MoCo is skipped because it is not specified as a dependency of this project in pom.xml")
                        return
                    }
                    MoCoLogger.useMvnLog(log)
                    log.info("-----------------------------------------------------------------------")
                    log.info("                               M O C O")
                    log.info("-----------------------------------------------------------------------")
                    log.info("START")
                    log.info("Note: make sure to use MoCo after tests phase")
                    log.info("Maven session timestamp: ${session?.startTime?.time.toString()}")

                    // Often named as "target" or "build" folder, contains compiled classes, JaCoCo report, MoCo report, etc...
                    var rootProject = project
                    while (rootProject!!.hasParent()) {
                        rootProject = rootProject.parent
                    }
                    val s = File.separator

                    if (localRepository == null || localRepository.basedir.isNullOrEmpty()) {
                        log.info("Exit - MoCo can't detect your local git repository")
                        return
                    }

                    val property = "java.io.tmpdir"
                    var tempDir = System.getProperty(property)
                    log.info("DB directory is $tempDir")
                    if (tempDir.last() != '/') tempDir += "/"
                    val persistencePath = tempDir + "moco${s}" + "${rootProject.groupId}-${rootProject.artifactId}" + "${s}moco"
                    // localRepository.basedir + "${s}io${s}moco${s}" + rootProject.artifactId + "${s}persistence"

                    log.info("Configured compiled code directory: ${if (codeRootDir.isNotEmpty()) codeRootDir else "default"}")
                    log.info("Configured compiled test directory: ${if (testRootDir.isNotEmpty()) testRootDir else "default"}")
                    val buildRoot = project?.build?.directory.toString()
                    val codeRoot = project?.build?.outputDirectory.toString()
                    var codeTarget = codeRoot
                    if (codeRootDir.isNotEmpty()) {
                        val temp = "$codeTarget$s$codeRootDir"
                        if (File(temp).exists()) {
                            codeTarget = temp
                        } else {
                            log.info("Exit - Configured compiled sources directory is not found in this project")
                            return
                        }
                    }

                    val testRoot = project?.build?.testOutputDirectory.toString()
                    var testTarget = testRoot
                    if (testRootDir.isNotEmpty()) {
                        val temp = "$testTarget$s$testRootDir"
                        if (File(temp).exists()) {
                            testTarget = temp
                        } else {
                            log.info("Exit - Configured compiled tests directory is not found in this project")
                            return
                        }
                    }

                    val classPath = prepareAllClassPaths(rootProject)
                    val jvm = System.getProperty("java.home") + s + "bin" + s + "java"
                    val mocoBuildPath = "$buildRoot${s}$mocoRoot"
                    val fOpNames = Operator.supportedOperatorNames.filter { !excludedMuOpNames.contains(it) }

                    val rootProjectBaseDir = if (rootProject.basedir != null) rootProject.basedir.toString()
                    else Paths.get("").toAbsolutePath().toString()

                    val configuration = Configuration(
                        rootProjectBaseDir = rootProjectBaseDir,
                        mavenSession = session?.startTime?.time.toString(),
                        buildRoot = buildRoot,
                        codeRoot = codeRoot,
                        testRoot = testRoot,
                        codeTarget = codeTarget,
                        testTarget = testTarget,
                        mocoBuildPath = mocoBuildPath,
                        excludedSourceClasses = excludedSourceClasses,
                        excludedSourceFolders = excludedSourceFolders,
                        excludedTestClasses = excludedTestClasses,
                        excludedTestFolders = excludedTestFolders,
                        classPath = classPath,
                        jvm = jvm,
                        preprocessResultsFolder = preprocessResultsFolder,
                        mutationResultsFolder = mutationResultsFolder,
                        excludedMuOpNames = excludedMuOpNames,
                        fOpNames = fOpNames,
                        baseDir = project?.basedir.toString(),
                        compileSourceRoots = project?.compileSourceRoots,
                        groupId = project?.groupId!!,
                        artifactId = project?.artifactId!!,
                        gitMode = gitMode,
                        preprocessTestTimeout = preprocessTestTimeout,
                        mutationPerClass = 0,
                        filterMutants = filterMutants,
                        debugEnabled = debugEnabled,
                        verbose = verbose,
                        numberOfThreads = numberOfThreads,
                        noLogAtAll = false,
                        enableMetrics = enableMetrics,
                        useForCICD = useForCICD,
                        mocoPluginVersion = mojo?.plugin?.version
                    )

                    Configuration.currentConfig = configuration
                    MoCoLogger.debugEnabled = Configuration.currentConfig!!.debugEnabled
                    H2Database.initPool(
                        url = "jdbc:h2:file:${persistencePath};mode=MySQL;",
                        user = "moco",
                        password = "moco",
                    )
                    dbON = true
                    H2Database().initDBTablesIfNotExists()
                    log.info("Project: GroupID - ${project?.groupId!!}, artifactId - ${project?.artifactId!!}")
                    MoCoEntryPoint(configuration).execute()
                } else log.info("MoCo cannot detect your project, please check the configuration in pom.xml")
            } else {
                log.info("MoCo is currently disabled in this project pom.xml file")
            }
        } catch (e: Exception) {
            log.error(e.printStackTrace().toString())
        } finally {
            if (dbON) H2Database.shutDownDB()
        }
    }


    private fun prepareAllClassPaths(rootProject: MavenProject?): String {
        // Loop through the hierarchy of maven project and add all relevant classpaths
        val buildRoot = project?.build?.directory.toString()
        val codeRoot = project?.build?.outputDirectory.toString()
        val testRoot = project?.build?.testOutputDirectory.toString()
        val runtimeCp = project?.runtimeClasspathElements
        val compileCp = project?.compileClasspathElements
        val testCompileCp = project?.testClasspathElements
        val runtimeRootCp = rootProject?.runtimeClasspathElements
        val compileRootCp = rootProject?.compileClasspathElements
        val testCompileRootCp = rootProject?.testClasspathElements

        val resPath = prepareTestDependenciesPath()
            .union(mutableSetOf(buildRoot, codeRoot, testRoot))
            .union(System.getProperty("java.class.path").split(File.pathSeparator).toSet())
            .union(compileCp!!.toSet())
            .union(compileRootCp!!.toSet())
            .union(runtimeCp!!.toSet())
            .union(runtimeRootCp!!.toSet())
            .union(testCompileRootCp!!.toSet())
            .union(testCompileCp!!.toSet())
            .toMutableSet()
        val collectedProjects = rootProject.collectedProjects
        if (!collectedProjects.isNullOrEmpty()) {
            for (p in collectedProjects) {
                val runtime = p.runtimeClasspathElements
                val compile = p.compileClasspathElements
                val testCompile = p.testClasspathElements
                resPath.addAll(compile.toSet())
                resPath.addAll(runtime.toSet())
                resPath.addAll(testCompile.toSet())
            }
        }
        return resPath.joinToString(separator = File.pathSeparator)
    }

    private fun prepareTestDependenciesPath(): MutableSet<String> {
        val s = File.separator
        val root = localRepository?.basedir.toString()
        val res: MutableSet<String> = mutableSetOf()
        val depNames: List<List<String>> = listOf(
            listOf("org${s}junit${s}platform", "junit-platform-launcher", "1.7.0"),
            listOf("org${s}junit${s}platform", "junit-platform-suite-api", "1.6.2"),
            listOf("org${s}junit${s}jupiter", "junit-jupiter", "5.7.0"),
            listOf("junit", "junit", "4.12"),
            listOf("org${s}testng", "testng", "6.9.10")
        )
        depNames.map {
            val depPath = root + s + it[0] + s + it[1] + s + it[2] + s + it[1] + "-" + it[2] + ".jar"
            val depFile = File(depPath)
            if (depFile.exists()) {
                res.add(depPath)
            }
        }
        return res
    }
}

