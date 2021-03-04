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
    private var byteArrLoader: ByteArrayLoader
    private var agentLoc: String? = null
    private val mutationStorage: MutationStorage = MutationStorage(mutableMapOf())
    private var clsByGit: List<String>? = listOf()
    private var projectMeta: ProjectMeta? = null
    private var recordedTestMapping: String? = null
    private var gitProcessor = GitProcessor(configuration.baseDir)
    private var newOperatorsSelected: Boolean = false

    init {
        MoCoLogger.verbose = configuration.verbose
        logger.info("-----------------------------------------------------------------------")
        logger.info("                               M O C O")
        logger.info("-----------------------------------------------------------------------")
        logger.info("START")
        projectMeta = ProjectMeta()
        byteArrLoader = ByteArrayLoader(classPath)
    }

    fun execute() {
        val executionTime = measureTimeMillis {
            if (!initMoCoOK()) logger.info("EXIT: Nothing to do")
            else {
                logger.info("Preprocessing started......")
                PreprocessEntryPoint().preprocessing(clsByGit!!, recordedTestMapping, agentLoc!!)
                logger.info("Preprocessing completed")
                logger.info("Mutation Test started......")
                MutationEntryPoint(byteArrLoader, mutationStorage, gitProcessor, agentLoc, clsByGit).mutationTest(newOperatorsSelected)
                logger.info("Mutation Test completed")
            }
        }
        logger.info("Execution done after ${executionTime / 1000}s")
        Metrics(mutationStorage).reportResults(fOpNames, gitProcessor)
        cleanBeforeExit()
        logger.info("DONE")
    }

    private fun cleanBeforeExit() {
        // Save meta before exit
        logger.debug("Saving project meta data before exiting...")
        if (gitMode) {
            gitProcessor.setHeadCommitMeta(projectMeta!!)
            projectMeta!!.meta["sourceBuildFolder"] = configuration.codeRoot
            projectMeta!!.meta["testBuildFolder"] = configuration.testRoot

            if (newOperatorsSelected) {
                projectMeta!!.meta["runOperators"] += "-" + fOpNames.joinToString(",")
            }
            projectMeta?.saveMetaData()
        }
        // Remove generated agent after finishing
        JarUtil.removeTemporaryAgentJar(agentLoc)
        logger.debug("Remove temporary moco agent jar successfully")
    }

    private fun initMoCoOK(): Boolean {
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

        agentLoc = JarUtil.createTemporaryAgentJar(byteArrLoader)
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

    private fun gitInfoProcessing(): Boolean {
        if (!shouldRunFromScratch()) {
            logger.info("Git mode: on")
            logger.info("Latest stored commit: ${projectMeta?.meta?.get("latestStoredCommitID")}")

            if (projectMeta?.meta?.get("latestStoredCommitID").isNullOrEmpty()) {
                clsByGit = listOf("")
                logger.info("Last commit info does not exist - skip Git commits diff analysis - proceed in normal mode")
            } else {
                clsByGit = gitProcessor.getChangedClsSinceLastStoredCommit(
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

    private fun shouldResetDB(): Boolean  {
        if (!gitMode) {
            return false
        }
        val sourceBuildFolder = projectMeta?.meta?.get("sourceBuildFolder")
        val recordedTestFolder = projectMeta?.meta?.get("testBuildFolder")
        if (sourceBuildFolder != Configuration.currentConfig?.buildRoot) {
            return true
        }
        if (recordedTestFolder != Configuration.currentConfig?.testRoot) {
            return true
        }
        return false
    }

    private fun shouldRunFromScratch(): Boolean {
        // MoCo will rerun (execute all tests on all cut when users change mutation operator set configuration
        val recordedOps = projectMeta?.meta?.get("runOperators")?.split("-")
        if (!recordedOps.isNullOrEmpty()) {
            newOperatorsSelected = !recordedOps.contains(fOpNames.joinToString(","))
        }
        if (newOperatorsSelected) {
            logger.info("New set of operators are chosen, do mutation tests without filtering")
            return true
        }
        if (!gitMode) {
            logger.info("Git mode: off")
            return true
        }

        return false
    }
}