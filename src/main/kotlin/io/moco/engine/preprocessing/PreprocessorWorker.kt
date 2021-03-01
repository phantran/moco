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
import io.moco.engine.test.TestItem
import io.moco.engine.test.TestItemWrapper
import io.moco.utils.JsonConverter
import io.moco.utils.MoCoLogger
import java.net.Socket
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
        var socket: Socket? = null
        val buildRoot = args[1]  // path to build or target folder of project
        val codeRoot = args[2]
        val testRoot = args[3]
        val excludedSourceClasses = if (args[4] != "") args[4].split(",").map { it.trim() } else listOf()
        val excludedSourceFolders = if (args[5] != "") args[5].split(",").map { it.trim() } else listOf()
        val excludedTestClasses = if (args[6] != "") args[6].split(",").map { it.trim() } else listOf()
        val excludedTestFolders = if (args[7] != "") args[7].split(",").map { it.trim() } else listOf()
        val preprocessResultFileName = args[8]
        TestItemWrapper.configuredTestTimeOut = if (args[9].toIntOrNull() != null) args[9].toLong() else -1
        MoCoLogger.debugEnabled = args[10] == "true"
        MoCoLogger.verbose = args[11] == "true"
        // this list is null of empty if git mode changed classes if off or no changed classes are detected
        val filteredClsByGitCommit =
            if (args[12] != "") args[12].split(",").map { it.trim() } else null
        val isRerun = if (args.getOrNull(13) != null) args[13].toBoolean() else false
        val jsonConverter = JsonConverter("$buildRoot/moco/preprocess/", preprocessResultFileName)

        MoCoLogger.useKotlinLog()
        val logger = MoCoLogger()

        try {
            socket = Socket("localhost", args[0].toInt())
            if (isRerun) logger.info("Continue processing step due to the previous test error")
            val analysedCodeBase = Codebase(
                codeRoot, testRoot, excludedSourceClasses, excludedSourceFolders,
                excludedTestClasses, excludedTestFolders, filteredClsByGitCommit
            )
            logger.info("Preprocessing: ${analysedCodeBase.sourceClassNames.size} source classes found")

            if (analysedCodeBase.testClassesNames.size == 0) {
                logger.info("Preprocessing: No new tests to run")
                return
            }
            MocoAgent.addTransformer(PreprocessorTransformer(analysedCodeBase.sourceClassNames))
            Preprocessor(analysedCodeBase).preprocessing(isRerun, jsonConverter)

            jsonConverter.savePreprocessToJson(PreprocessorTracker.getPreprocessResults())
            logger.info("Preprocessing: Data saved and exit")
            exitProcess(MoCoProcessCode.OK.code)

        } catch (ex: Exception) {
            jsonConverter.savePreprocessToJson(PreprocessorTracker.getPreprocessResults())
            logger.info("Preprocessing: Data saved and exit")
            logger.info("Preprocessing: Exit because of error")
            socket?.close()
            val exitIndex = ex.message!!.toIntOrNull()
            if (exitIndex != null) {
                exitProcess(exitIndex)
            } else {
                exitProcess(MoCoProcessCode.UNRECOVERABLE_ERROR.code)
            }
        }
    }
}