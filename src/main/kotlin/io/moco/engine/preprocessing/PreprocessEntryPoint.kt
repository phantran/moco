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

import io.moco.engine.Configuration
import io.moco.engine.MoCoProcessCode
import io.moco.engine.WorkerProcess
import io.moco.utils.MoCoLogger


class PreprocessEntryPoint {
    private val processWorkerArguments = Configuration.currentConfig!!.getPreprocessProcessArgs()
    private val classPath = Configuration.currentConfig!!.classPath
    val logger = MoCoLogger()

    fun preprocessing(filteredClsByGit: List<String>, recordedTestMapping: String?, agentLoc: String) {
        try {
            var processStatus = MoCoProcessCode.NOT_STARTED.code
            var previousStatus = MoCoProcessCode.OK.code

            processWorkerArguments.add(filteredClsByGit.joinToString(","))
            if (recordedTestMapping != null) processWorkerArguments.add(recordedTestMapping)
            else processWorkerArguments.add("")
            while (processStatus != MoCoProcessCode.OK.code) {
                val workerProcess = WorkerProcess(
                    PreprocessorWorker.javaClass, WorkerProcess.processArgs(agentLoc, classPath),
                    processWorkerArguments
                )
                workerProcess.start()
                processStatus = workerProcess.getProcess()?.waitFor()!!
                if (processStatus == MoCoProcessCode.UNRECOVERABLE_ERROR.code ||
                    previousStatus == processStatus
                ) break
                else {
                    previousStatus = processStatus
                    // Add more parameter to param to process to signal that this is a rerun because of previous error
                    if (processWorkerArguments.getOrNull(17) == null) processWorkerArguments.add("true")
                }
            }
            if (processStatus != MoCoProcessCode.OK.code) {
                logger.warn("Please check you test suite because there might be erroneous tests in your test suite")
            }
        } catch (ex: Exception) {
            logger.error("Error while executing preprocessing phase - ${ex.message}")
        }
    }
}