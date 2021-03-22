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
import io.moco.engine.MoCoAgent
import io.moco.engine.test.TestItemWrapper
import io.moco.persistence.JsonSource
import io.moco.utils.MoCoLogger
import java.io.File
import kotlin.system.exitProcess


object PreprocessorWorker {
    lateinit var mocoBuildPath: String
    lateinit var codeRoot: String
    lateinit var testRoot: String
    lateinit var excludedSourceClasses: List<String>
    lateinit var excludedSourceFolders: List<String>
    lateinit var excludedTestClasses: List<String>
    lateinit var excludedTestFolders: List<String>
    lateinit var preprocessResultsFolder: String
    var recordedTestMapping: List<String>? = null
    var isRerun: Boolean = false
    // filteredClsByGitCommit is null of empty if
    // 1. Git mode is off
    // 2. Project run for the first time and no project meta exists
    var filteredClsByGitCommit: List<String>? = null
    val logger = MoCoLogger()
    lateinit var jsonConverter: JsonSource
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
        prepareWorker(args)
        try {
            if (isRerun) logger.debug("Continue processing step due to the previous test error")
            val analysedCodeBase = Codebase(
                codeRoot, testRoot, excludedSourceClasses,
                excludedSourceFolders, excludedTestClasses, excludedTestFolders
            )
            logger.debug("Preprocessing: Code base has ${analysedCodeBase.sourceClassNames.size} source classes")

            MoCoAgent.addTransformer(PreprocessorTransformer(analysedCodeBase.sourceClassNames))
            val relevantTests = getRelevantTests(filteredClsByGitCommit, analysedCodeBase, recordedTestMapping)

            // Process after filtering
            if (relevantTests.isNotEmpty()) {
                // filter by git is null which means proceed in normal processing
                Preprocessor(relevantTests).preprocessing(isRerun, jsonConverter)
                jsonConverter.savePreprocessToJson(PreprocessorTracker.getPreprocessResults())
                logger.debug("Preprocessing: Data saved and exit")
            } else {
                logger.debug("Preprocessing: 0 relevant tests left after filtering")
                logger.debug("Preprocessing: Exit - nothing to run")
            }
            exitProcess(MoCoProcessCode.OK.code)
        } catch (ex: Exception) {
            jsonConverter.savePreprocessToJson(PreprocessorTracker.getPreprocessResults())
            logger.debug("Preprocessing: Data saved and exit")
            logger.debug("Preprocessing: Exit because of error")
            if (!ex.message.isNullOrEmpty()) {
                val exitIndex = ex.message!!.toIntOrNull()
                if (exitIndex != null) {
                    exitProcess(exitIndex + 1)
                }
            }
            exitProcess(MoCoProcessCode.UNRECOVERABLE_ERROR.code)
        }
    }

    fun prepareWorker(args: Array<String>) {
        mocoBuildPath = args[1]  // path to build or target folder of project
        codeRoot = args[2]
        testRoot = args[3]
        excludedSourceClasses = if (args[4] != "") args[4].split(",").map { it.trim() } else listOf()
        excludedSourceFolders = if (args[5] != "") args[5].split(",").map { it.trim() } else listOf()
        excludedTestClasses = if (args[6] != "") args[6].split(",").map { it.trim() } else listOf()
        excludedTestFolders = if (args[7] != "") args[7].split(",").map { it.trim() } else listOf()
        preprocessResultsFolder = args[8]
        TestItemWrapper.configuredTestTimeOut = if (args[9].toIntOrNull() != null) args[9].toLong() else -1L
        MoCoLogger.debugEnabled = args[10] == "true"
        MoCoLogger.verbose = args[11] == "true"
        if (args[12] == "true") MoCoLogger.noLogAtAll = true
        // filteredClsByGitCommit is null of empty if
        // 1. Git mode is off
        // 2. Project run for the first time and no project meta exists
        filteredClsByGitCommit =
            if (args[13] != "") args[13].split(",").map { it.trim() } else null
        recordedTestMapping = if (args[14] != "") args[14].split(",").map { it.trim() } else null
        isRerun = if (args.getOrNull(15) != null) args[15].toBoolean() else false
        MoCoLogger.useKotlinLog()
        jsonConverter = JsonSource(
            "$mocoBuildPath${File.separator}$preprocessResultsFolder", "preprocess"
        )
    }

    fun getRelevantTests(
        filteredClsByGitCommit: List<String>?, codebase: Codebase,
        recordedTestMapping: List<String>?
    ): List<ClassName> {
        // Return list of relevant tests to be executed
        // tests that are changed according to git diff and tests that were mapped before to
        // corresponding changed source classes
        logger.debug("Recorded test relevant tests from previous runs: $recordedTestMapping")
        logger.debug("Changed classes by Git: $filteredClsByGitCommit")
        val res = if (!filteredClsByGitCommit.isNullOrEmpty()) {
            codebase.testClassesNames.filter {
                filteredClsByGitCommit.any{ it1 -> it1.contains(it.name) }
            }.toMutableSet()
        } else codebase.testClassesNames
        if (recordedTestMapping != null) res.addAll(recordedTestMapping.map { ClassName(it) })
        return res.toList()
    }
}
