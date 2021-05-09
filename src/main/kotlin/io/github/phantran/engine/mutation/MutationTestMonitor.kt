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

package io.github.phantran.engine.mutation

import io.github.phantran.engine.ClassName
import io.github.phantran.engine.test.TestItemWrapper


class MutationTestMonitor {
    private var timeoutMutationTypeList: MutableSet<TimeoutMutationType> = mutableSetOf()

    internal data class TimeoutMutationType(
        val lineOfCode: Int, val operatorName: String,
        val className: ClassName, var count: Int = 0
    )

    var blacklistedTests: MutableMap<String, MutableSet<String>> = mutableMapOf()

    fun updateBlackListedTests(test: TestItemWrapper?, mutation: Mutation) {
        if (test == null) return
        val testName = test.testItem.toString()
        if (blacklistedTests.keys.contains(testName)) {
            blacklistedTests[testName]?.add("${mutation.mutationID.location.className}-${mutation.lineOfCode}")
        } else {
            blacklistedTests[testName] = mutableSetOf("${mutation.mutationID.location.className}-${mutation.lineOfCode}")
        }
    }

    fun markTimeoutMutationType(mutation: Mutation) {
        val temp = timeoutMutationTypeList.find { sameTypeOfMutation(it, mutation) }
        if (temp != null) {
            temp.count += 1
        } else {
            timeoutMutationTypeList.add(
                TimeoutMutationType(
                    mutation.lineOfCode, mutation.mutationID.operatorName,
                    mutation.mutationID.location.className!!, 1
                )
            )
        }
    }

    fun shouldSkipThisMutation(mutation: Mutation): Boolean {
        return timeoutMutationTypeList.any { sameTypeOfMutation(it, mutation) && it.count > 1 }
    }

    private fun sameTypeOfMutation(a: TimeoutMutationType, b: Mutation): Boolean {
        // same class, same line of code, same type of mutation operator
        if (a.lineOfCode == b.lineOfCode &&
            a.operatorName == b.mutationID.operatorName &&
            a.className == b.mutationID.location.className
        ) {
            return true
        }
        return false
    }
}