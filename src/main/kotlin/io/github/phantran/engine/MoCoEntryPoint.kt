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

package io.github.phantran.engine

import io.github.phantran.utils.ByteArrayLoader
import io.github.phantran.engine.mutation.*
import io.github.phantran.engine.preprocessing.PreprocessEntryPoint
import io.github.phantran.utils.GitProcessor
import io.github.phantran.persistence.*
import io.github.phantran.utils.JarUtil
import java.io.File
import io.github.phantran.utils.MoCoLogger
import kotlin.jvm.Throws
import kotlin.system.measureTimeMillis


class MoCoEntryPoint(private val configuration: Configuration) {
    // Preprocessing step: Parse the targets source code and tests to collect information
    // about code blocks and mapping from classes under test to test classes
    // (which test classes are responsible for which classes under test)
    private val logger = MoCoLogger()
    private val classPath: String = configuration.classPath
    private var fOpNames = configuration.fOpNames
    private var preprocessResultsFolder = configuration.preprocessResultsFolder
    private var excludedSourceClasses = configuration.excludedSourceClasses
    private var excludedTestClasses = configuration.excludedTestClasses
    private var excludedSourceFolders = configuration.excludedSourceFolders
    private var excludedTestFolders = configuration.excludedTestFolders
    private var excludedMuOpNames = configuration.excludedMuOpNames
    private var mocoBuildPath = configuration.mocoBuildPath
    private var gitMode = configuration.gitMode
    private lateinit var byteLoader: ByteArrayLoader
    private var agentLoc: String? = null
    private var runID: String = System.currentTimeMillis().toString()

    @get: Synchronized
    private val mutationStorage: MutationStorage =
        MutationStorage(mutableMapOf(), this.runID)
    private var clsByGit: List<String>? = listOf()
    private var projectMeta: ProjectMeta? = null
    private var recordedTestMapping: String? = null
    private var gitProcessor: GitProcessor? = null
    private var newOp: Boolean = false

    companion object {
        var runScore = 0.0
    }

    fun execute() {
        try {
            val executionTime = measureTimeMillis {
                if (!initMoCoOK()) {
                    logger.info("EXIT: Nothing to do")
                    return
                } else {
                    logger.info("Preprocessing started......")
                    PreprocessEntryPoint().preprocessing(clsByGit!!, recordedTestMapping, agentLoc!!)
                    logger.info("Preprocessing completed")
                    logger.info("Mutation Test started......")
                    MutationEntryPoint(
                        byteLoader, mutationStorage, gitProcessor, agentLoc,
                        clsByGit, projectMeta?.meta?.get("lastRunID")
                    ).mutationTest(newOp)
                    logger.info("Mutation Test completed")
                }
            }
            logger.info("Execution done after ${executionTime / 1000}s")
            if (Configuration.currentConfig!!.enableMetrics) {
                Metrics(mutationStorage).reportResults(fOpNames, gitProcessor)
            }
            cleanBeforeExit()
            logger.info("DONE")
        } catch (ex: Exception) {
            JarUtil.removeTemporaryAgentJar(agentLoc)
            logger.debug("Remove temporary moco agent jar successfully")
            throw ex
        }
    }

    @Throws(Exception::class)
    private fun initMoCoOK(): Boolean {
        MoCoLogger.verbose = configuration.verbose
        if (!gitMode) logger.info("Git mode: OFF")
        byteLoader = ByteArrayLoader(classPath)
        if (fOpNames.isEmpty()) {
            // List of mutation operators after filtering is empty
            return false
        }
        if (shouldResetDB()) {
            logger.info("First run...")
            H2Database().dropAllMoCoTables()
            H2Database().initDBTablesIfNotExists()
        }
        projectMeta = ProjectMeta()
        logger.info("${fOpNames.size} selected mutation operators: " + fOpNames.joinToString(", "))
        if (excludedMuOpNames != "") logger.info("Excluded operators: $excludedMuOpNames")
        if (excludedSourceClasses != "") logger.infoVerbose("Excluded sources classes: $excludedSourceClasses")
        if (excludedSourceFolders != "") logger.infoVerbose("Excluded source folder: $excludedSourceFolders")
        if (excludedTestClasses != "") logger.infoVerbose("Excluded test classes: $excludedTestClasses")
        if (excludedTestFolders != "") logger.infoVerbose("Excluded test folder: $excludedTestFolders")

        val gitOK = gitInfoProcessing()
        if (!gitOK) return false

        agentLoc = JarUtil.createTemporaryAgentJar(byteLoader)
        if (agentLoc == null) {
            logger.error("Error while creating MoCo Agent Jar")
            return false
        }

        // Clear preprocessing JSON if exists
        JsonSource(
            "${mocoBuildPath}${File.separator}${preprocessResultsFolder}",
            "preprocess"
        ).removeJSONFileIfExists()
        return true
    }

    private fun gitInfoProcessing(): Boolean {
        if (gitMode) {
            logger.info("Git mode: ON")
            try {
                gitProcessor = GitProcessor(configuration.rootProjectBaseDir)
            } catch (e: Exception) {
                logger.error(
                    "Git Mode is ON but Git information couldn't be processed, " +
                            "make sure Git exits for this project"
                )
                logger.error(e.printStackTrace().toString())
                return false
            }
        }

        if (!shouldRunFromScratch()) {
            if (projectMeta?.meta?.get("storedHeadCommit").isNullOrEmpty()) {
                clsByGit = listOf("")
                logger.info("Recorded commit id does not exist - skip Git commits diff analysis - proceed in normal mode")
            } else {
                logger.info("Recorded commit: ${projectMeta?.meta?.get("storedHeadCommit")}")
                clsByGit = gitProcessor!!.getChangedClsSinceLastStoredCommit(
                    configuration.groupId.replace(".", File.separator), projectMeta?.meta!!
                )
                if (clsByGit != null) {
                    if (clsByGit?.isEmpty() == true) {
                        logger.info("Preprocessing: Git mode - No changed files found between recorded commit and head commit")
                        if (Configuration.currentConfig?.useForCICD == true) {
                            JsonSource.createMoCoJsonIfBuildFolderWasCleaned(
                                Configuration.currentConfig?.buildRoot,
                                mocoBuildPath,
                                Configuration.currentConfig!!.mutationResultsFolder,
                                runID
                            )
                        }
                        return false
                    }
                    logger.info("Preprocessing: Git mode - ${clsByGit!!.size} changed class(es) between recorded commit and head commit")
                    logger.debug("Classes found: $clsByGit")
                    recordedTestMapping = TestsCutMapping().getRecordedMapping(clsByGit!!)
                } else {
                    // clsByGit equals null could mean database has been reset or it's the first run
                    // if it's null then we set it to empty list as below
                    clsByGit = listOf("")
                    logger.info("Preprocessing: Git mode - recorded commit not found - proceed in normal mode")
                }
            }
        }
        return true
    }

    private fun shouldResetDB(): Boolean {
        // Reset if different version of MoCo
        val recordedMocoVersion = projectMeta?.meta?.get("mocoVersion") ?: return false
        if (Configuration.currentConfig?.mocoPluginVersion != recordedMocoVersion) {
            return true
        }
        // MoCo will not used database if Git Mode is off -> proceed in normal mode
        // No execution data and meta data will be collected
        if (!gitMode) return false
        val sourceBuildFolder = projectMeta?.meta?.get("sourceBuildFolder") ?: return false
        val recordedTestFolder = projectMeta?.meta?.get("testBuildFolder") ?: return false
        val recordedArtifactId = projectMeta?.meta?.get("artifactId") ?: return false
        val recordedGroupId = projectMeta?.meta?.get("groupId") ?: return false
        // If one changes the target build folder and test folder -> MoCo will erase the existing database and
        // run from scratch to avoid saving conflicting mutation data to database
        if (sourceBuildFolder != Configuration.currentConfig?.buildRoot) return true
        if (recordedTestFolder != Configuration.currentConfig?.testRoot) return true
        if (recordedArtifactId != Configuration.currentConfig?.artifactId) return true
        if (recordedGroupId != Configuration.currentConfig?.groupId) return true
        return false
    }

    fun shouldRunFromScratch(): Boolean {
        if (gitMode) {
            if (projectMeta?.meta?.get("lastMavenSessionID") == Configuration.currentConfig?.mavenSession) {
                if (projectMeta?.meta?.get("storedPreviousHeadCommit").isNullOrEmpty()) {
                    // First run (already run for the first child maven project with corresponding pom.xml)
                    logger.info("No recorded head commit in database - run from scratch")
                    return true
                }
            }
            // Run from scratch if mutation operators inclusion/exclusion configuration was changed
            val recordedOps = projectMeta?.meta?.get("runOperators")?.split("-")
            if (!recordedOps.isNullOrEmpty()) {
                if (!recordedOps.contains(fOpNames.joinToString(","))) {
                    logger.info("First run or changed configured mutation operators - run from scratch")
                    newOp = true
                    return true
                }
            }
        } else return true
        return false
    }

    private fun cleanBeforeExit() {
        // Remove generated agent after finishing
        JarUtil.removeTemporaryAgentJar(agentLoc)
        // Save meta before exit
        logger.debug("Saving project meta data before exiting...")
        projectMeta!!.meta["lastRunID"] = mutationStorage.runID
        if (gitMode) gitProcessor!!.setHeadCommitMeta(projectMeta!!, gitMode)
        projectMeta!!.meta["lastMavenSessionID"] = configuration.mavenSession
        projectMeta!!.meta["sourceBuildFolder"] = configuration.codeRoot
        projectMeta!!.meta["testBuildFolder"] = configuration.testRoot
        projectMeta!!.meta["artifactId"] = configuration.artifactId
        projectMeta!!.meta["groupId"] = configuration.groupId
        projectMeta!!.meta["mocoVersion"] = configuration.mocoPluginVersion!!

        if (newOp) {
            projectMeta!!.meta["runOperators"] += "-" + fOpNames.joinToString(",")
        }
        projectMeta?.saveMetaData()
        logger.debug("Remove temporary moco agent jar successfully")
    }
}