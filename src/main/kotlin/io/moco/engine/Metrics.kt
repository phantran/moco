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

import io.moco.persistence.MutationStorage
import io.moco.persistence.ProgressClassTest
import io.moco.persistence.ProjectTestHistory
import io.moco.utils.GitProcessor
import io.moco.utils.MoCoLogger

class Metrics(private val mutationStorage: MutationStorage) {
    private val logger = MoCoLogger()

    private fun calculateRunCoverage(mutationStorage: MutationStorage): Double {
        var total = 0.0
        var killedMutants = 0.0
        for ((_, value) in mutationStorage.entries) {
            total += value.size
           for (item in value) {
               if (item["result"] as String == "killed") killedMutants += 1
           }
        }
        var res = 0.0
         if (total > 0.0) {
             res = (killedMutants / total) * 100.0
         }
        return res
    }

    private fun calculateAccumulatedCoverage(configuredOperators: String): Double {
        val savedProgress = ProgressClassTest().getData("covered_operators = \'$configuredOperators\'")
        var killedMutants = 0.0
        var totalMutants = 0.0
        for (item in savedProgress) {
            killedMutants += item.entry["killed_mutants"]?.toIntOrNull() ?: 0
            totalMutants += item.entry["total_mutants"]?.toIntOrNull() ?: 0
        }
        return if (totalMutants == 0.0) 0.0
               else ((killedMutants / totalMutants) * 100)
    }

    fun reportResults(filteredMuOpNames: List<String>, gitProcessor: GitProcessor) {
        val runCoverage = calculateRunCoverage(mutationStorage)
        logger.info("-----------------------------------------------------------------------")
        logger.info("Mutation Coverage of this run: " + "%.2f".format(runCoverage) + "%")
        if (Configuration.currentConfig!!.gitMode) {
            val accumulatedCoverage = calculateAccumulatedCoverage(filteredMuOpNames.joinToString(","))
            logger.info("Accumulated Coverage for current configuration: " + "%.2f".format(accumulatedCoverage))
            // save this run to run history
            logger.debug("Saving new entry to project history")
            val temp = ProjectTestHistory()
            temp.entry = mutableMapOf(
                "commit_id" to gitProcessor.headCommit.name,
                "branch" to gitProcessor.branch, "run_operators" to filteredMuOpNames.joinToString(","),
                "run_coverage" to runCoverage.toString(), "accumulated_coverage" to accumulatedCoverage.toString(),
                "git_mode" to Configuration.currentConfig!!.gitMode.toString()
            )
            temp.save()
        }
        logger.info("-----------------------------------------------------------------------")


    }
}