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

object Metrics {
    fun calculateRunCoverage(mutationStorage: MutationStorage): Float {
        var total = 0
        var killedMutants = 0
        for ((_, value) in mutationStorage.entries) {
            total += value.size
           for (item in value) {
               if (item["result"] as String == "killed") killedMutants += 1
           }
        }
        return if (total == 0) 0f
               else (killedMutants/total)*100f
    }

    fun calculateAccumulatedCoverage(configuredOperators: String): Float {
        val savedProgress = ProgressClassTest().getData("covered_operators IN ('$configuredOperators')")
        var killedMutants = 0
        var totalMutants = 0
        for (item in savedProgress) {
            killedMutants += item.entry["killed_mutants"]?.toIntOrNull() ?: 0
            totalMutants += item.entry["totalMutants"]?.toIntOrNull() ?: 0
        }
        return if (totalMutants == 0) 0f
               else (killedMutants/totalMutants)*100f
    }
}