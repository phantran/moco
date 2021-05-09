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

import io.github.phantran.persistence.MutationStorage
import io.github.phantran.persistence.ProgressClassTest
import io.github.phantran.persistence.ProjectTestHistory
import io.github.phantran.utils.GitProcessor
import io.github.phantran.utils.MoCoLogger


class Metrics(private val mutationStorage: MutationStorage) {
    val logger = MoCoLogger()

    fun calculateRunCoverage(mutationStorage: MutationStorage): Double {
        var total = 0.0
        var killedMutants = 0.0
        for ((_, value) in mutationStorage.entries) {
            total += value.count { it["result"] != "run_error" }
            killedMutants += value.count { it["result"] == "killed" }
        }
        logger.info("Total mutants in this run: ${total.toInt()}")
        logger.info("Killed mutants in this run: ${killedMutants.toInt()}")

        var res = 0.0
        if (total > 0.0) {
            res = (killedMutants / total) * 100.0
        }
        return res
    }

    private fun calculateAccumulatedCoverage(configuredOperators: String): Double {
        val savedProgress = ProgressClassTest().getData("coveredOperators = \'$configuredOperators\'")
        var killedMutants = 0.0
        var totalMutants = 0.0
        for (item in savedProgress) {
            killedMutants += item.entry["killedMutants"]?.toIntOrNull() ?: 0
            totalMutants += item.entry["totalMutants"]?.toIntOrNull() ?: 0
        }
        return if (totalMutants == 0.0) 0.0
        else ((killedMutants / totalMutants) * 100)
    }

    fun reportResults(filteredMuOpNames: List<String>, gitProcessor: GitProcessor?) {
        val runCoverage = calculateRunCoverage(mutationStorage)
        MoCoEntryPoint.runScore = runCoverage
        logger.info("-----------------------------------------------------------------------")
        logger.info("Mutation Coverage of this run: " + "%.2f".format(runCoverage) + "%")
        if (Configuration.currentConfig!!.gitMode) {
            val accumulatedCoverage = calculateAccumulatedCoverage(filteredMuOpNames.joinToString(","))
            logger.info("Accumulated Coverage for current configuration: " + "%.2f".format(accumulatedCoverage) + "%")
            // save this run to run history
            logger.debug("Saving new entry to project history")
            val temp = ProjectTestHistory()
            temp.entry = mutableMapOf(
                "commitID" to gitProcessor!!.headCommit.name,
                "branch" to gitProcessor.branch,
                "runOperators" to filteredMuOpNames.joinToString(","),
                "runCoverage" to runCoverage.toString(),
                "accumulatedCoverage" to accumulatedCoverage.toString(),
            )
            temp.save()
        }
        logger.info("-----------------------------------------------------------------------")
    }
}