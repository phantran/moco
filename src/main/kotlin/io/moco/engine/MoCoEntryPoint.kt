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

package io.moco.engine

import io.moco.utils.ByteArrayLoader
import io.moco.engine.mutation.*
import io.moco.engine.preprocessing.PreprocessEntryPoint
import io.moco.utils.GitProcessor
import io.moco.persistence.*
import io.moco.utils.JarUtil
import java.io.File

import io.moco.utils.MoCoLogger
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
    private val mutationStorage: MutationStorage =
        MutationStorage(mutableMapOf(), System.currentTimeMillis().toString())
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
                if (!initMoCoOK()) logger.info("EXIT: Nothing to do")
                else {
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
            throw ex
        }
    }

    @Throws(Exception::class)
    private fun initMoCoOK(): Boolean {
        MoCoLogger.verbose = configuration.verbose
        logger.info("-----------------------------------------------------------------------")
        logger.info("                               M O C O")
        logger.info("-----------------------------------------------------------------------")
        logger.info("START")
        if (!gitMode) logger.info("Git mode: OFF")
        projectMeta = ProjectMeta()
        byteLoader = ByteArrayLoader(classPath)
        if (fOpNames.isEmpty()) {
            // List of mutation operators after filtering is empty
            return false
        }
        logger.info("${fOpNames.size} selected mutation operators: " + fOpNames.joinToString(", "))
        if (excludedMuOpNames != "") logger.info("Excluded operators: $excludedMuOpNames")
        if (excludedSourceClasses != "") logger.info("Excluded sources classes: $excludedSourceClasses")
        if (excludedSourceFolders != "") logger.info("Excluded source folder: $excludedSourceFolders")
        if (excludedTestClasses != "") logger.info("Excluded test classes: $excludedTestClasses")
        if (excludedTestFolders != "") logger.info("Excluded test folder: $excludedTestFolders")

        val gitOK = gitInfoProcessing()
        if (!gitOK) return false

        agentLoc = JarUtil.createTemporaryAgentJar(byteLoader)
        if (agentLoc == null) {
            logger.info("Error while creating MoCo Agent Jar")
            return false
        }
        // Clear preprocessing JSON if exists
        JsonSource(
            "${mocoBuildPath}${File.separator}${preprocessResultsFolder}",
            "preprocess"
        ).removeJSONFileIfExists()

        if (shouldResetDB()) {
            H2Database().dropAllMoCoTables()
            H2Database().initDBTablesIfNotExists()
        }
        return true
    }

    private fun cleanBeforeExit() {
        // Remove generated agent after finishing
        JarUtil.removeTemporaryAgentJar(agentLoc)
        // Save meta before exit
        logger.debug("Saving project meta data before exiting...")
        projectMeta!!.meta["lastRunID"] = mutationStorage.runID
        if (gitMode) gitProcessor!!.setHeadCommitMeta(projectMeta!!, gitMode)

        projectMeta!!.meta["sourceBuildFolder"] = configuration.codeRoot
        projectMeta!!.meta["testBuildFolder"] = configuration.testRoot

        if (newOp) {
            projectMeta!!.meta["runOperators"] += "-" + fOpNames.joinToString(",")
        }
        projectMeta?.saveMetaData()
        logger.debug("Remove temporary moco agent jar successfully")
    }

    private fun gitInfoProcessing(): Boolean {
        if (gitMode) {
            try {
                gitProcessor = GitProcessor(configuration.baseDir)
            } catch (e: Exception) {
                logger.error(
                    "Git Mode is ON but Git information couldn't be processed, " +
                            "make sure this source code was initialized as a Git repository"
                )
                return false
            }
        }

        if (!shouldRunFromScratch()) {
            logger.info("Git mode: on")
            logger.info("Latest stored commit: ${projectMeta?.meta?.get("latestStoredCommitID")}")

            if (projectMeta?.meta?.get("latestStoredCommitID").isNullOrEmpty()) {
                clsByGit = listOf("")
                logger.info("Last commit info does not exist - skip Git commits diff analysis - proceed in normal mode")
            } else {
                clsByGit = gitProcessor!!.getChangedClsSinceLastStoredCommit(
                    configuration.artifactId.replace(".", "/"), projectMeta?.meta!!
                )
                if (clsByGit != null) {
                    if (clsByGit?.isEmpty() == true) {
                        logger.info("Preprocessing: Git mode: No changed files found by git commits diff")
                        // skip preprocessing in git mode and no detected changed class
                        return false
                    }
                    logger.info("Preprocessing: Git mode - ${clsByGit!!.size} changed classes by git commits diff")
                    logger.debug("Classes found: $clsByGit")
                    recordedTestMapping = projectMeta?.meta!!["latestStoredCommitID"]?.let {
                        TestsCutMapping().getRecordedMapping(clsByGit!!, it)
                    }
                } else {
                    clsByGit = listOf("")
                    logger.info("Preprocessing: Git mode: last stored commit not found - proceed in normal mode")
                }
            }
        }
        return true
    }

    private fun shouldResetDB(): Boolean {
        // MoCo will not used database if Git Mode is off -> proceed in normal mode
        // No execution data and meta data will be collected
        if (!gitMode) return false
        val sourceBuildFolder = projectMeta?.meta?.get("sourceBuildFolder")
        val recordedTestFolder = projectMeta?.meta?.get("testBuildFolder")
        // If one changes the target build folder and test folder -> MoCo will erase the existing database and
        // run from scratch to avoid saving conflicting mutation data to database
        if (sourceBuildFolder != Configuration.currentConfig?.buildRoot) return true
        if (recordedTestFolder != Configuration.currentConfig?.testRoot) return true
        return false
    }

    fun shouldRunFromScratch(): Boolean {
        if (gitMode) {
            // Run from scratch if mutation operators inclusion/exclusion configuration was changed
            val recordedOps = projectMeta?.meta?.get("runOperators")?.split("-")
            if (!recordedOps.isNullOrEmpty()) {
                if (!recordedOps.contains(fOpNames.joinToString(","))) {
                    newOp = true
                    return true
                }
            }
        } else {
            return true
        }
        return false
    }
}