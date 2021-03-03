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


package io.moco.engine.preprocessing

import io.moco.engine.ClassName
import io.moco.engine.Codebase
import io.moco.engine.MoCoProcessCode
import io.moco.engine.MocoAgent
import io.moco.engine.test.TestItemWrapper
import io.moco.persistence.JsonSource
import io.moco.utils.MoCoLogger
import java.io.File
import kotlin.system.exitProcess


object PreprocessorWorker {
    /**
     * Main
     *
     * @param args
     * Preprocessing to collect codebase information
     * This method in responsible for running test classes to collect the following information
     * 1. Mapping between test classes and source classes
     * 2. Mapping between covered line of codes and test classes
     * 3. Important information of line of codes such as: in finally block, in static init block...
     *
     * If GIT changed classes mode is on, preprocessing will be skipped if no changed test classes are
     * detected since last commit.
     *
     * The final result of this step is stored in preprocess JSON file and store in H2 persistent DB if DB mode is on
     * DB mode is on by default
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val mocoBuildPath = args[1]  // path to build or target folder of project
        val codeRoot = args[2]
        val testRoot = args[3]
        val excludedSourceClasses = if (args[4] != "") args[4].split(",").map { it.trim() } else listOf()
        val excludedSourceFolders = if (args[5] != "") args[5].split(",").map { it.trim() } else listOf()
        val excludedTestClasses = if (args[6] != "") args[6].split(",").map { it.trim() } else listOf()
        val excludedTestFolders = if (args[7] != "") args[7].split(",").map { it.trim() } else listOf()
        val preprocessResultsFolder = args[8]
        TestItemWrapper.configuredTestTimeOut = if (args[9].toIntOrNull() != null) args[9].toLong() else -1
        MoCoLogger.debugEnabled = args[10] == "true"
        MoCoLogger.verbose = args[11] == "true"
        // filteredClsByGitCommit is null of empty if
        // 1. Git mode is off
        // 2. Project run for the first time and no project meta exists
        val filteredClsByGitCommit =
            if (args[12] != "") args[12].split(",").map { it.trim() } else null
        val recordedTestMapping = if (args[13] != "") args[13].split(",").map { it.trim() } else null
        val isRerun = if (args.getOrNull(14) != null) args[14].toBoolean() else false


        val jsonConverter = JsonSource(
            "$mocoBuildPath${File.separator}$preprocessResultsFolder", "preprocess"
        )

        MoCoLogger.useKotlinLog()
        val logger = MoCoLogger()

        try {
            if (isRerun) logger.info("Continue processing step due to the previous test error")
            val analysedCodeBase = Codebase(
                codeRoot, testRoot, excludedSourceClasses,
                excludedSourceFolders, excludedTestClasses, excludedTestFolders
            )
            logger.debug("Preprocessing: Code base has ${analysedCodeBase.sourceClassNames.size} source classes")

            // Process after filtering
            if (analysedCodeBase.testClassesNames.size != 0) {
                // filter by git is null which means proceed in normal processing
                MocoAgent.addTransformer(PreprocessorTransformer(analysedCodeBase.sourceClassNames))
                val relevantTests = getRelevantTests(filteredClsByGitCommit, analysedCodeBase, recordedTestMapping)
                logger.info("Preprocessing: ${relevantTests.size} test classes left after filtering")
                Preprocessor(relevantTests).preprocessing(isRerun, jsonConverter)
                jsonConverter.savePreprocessToJson(PreprocessorTracker.getPreprocessResults())
                logger.info("Preprocessing: Data saved and exit")
            } else {
                logger.info("Preprocessing: Exit - nothing to run")
            }
            exitProcess(MoCoProcessCode.OK.code)
        } catch (ex: Exception) {
            jsonConverter.savePreprocessToJson(PreprocessorTracker.getPreprocessResults())
            logger.info("Preprocessing: Data saved and exit")
            logger.info("Preprocessing: Exit because of error")
            val exitIndex = ex.message!!.toIntOrNull()
            if (exitIndex != null) {
                exitProcess(exitIndex)
            } else {
                exitProcess(MoCoProcessCode.UNRECOVERABLE_ERROR.code)
            }
        }
    }

    private fun getRelevantTests(
        filteredClsByGitCommit: List<String>?, codebase: Codebase,
        recordedTestMapping: List<String>?
    ): List<ClassName> {
        // Return list of relevant tests to be executed
        // tests that are changed according to git diff and tests that were mapped before to
        // corresponding changed source classes
        val res = if (!filteredClsByGitCommit.isNullOrEmpty()) {
            codebase.testClassesNames.filter { filteredClsByGitCommit.contains(it.name) }.toMutableSet()
        } else {
            codebase.testClassesNames
        }

        if (recordedTestMapping != null) {
            res.addAll(recordedTestMapping.map { ClassName(it) })
        }
        return res.toList()
    }
}
