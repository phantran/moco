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

package io.moco.engine.mutation

import io.moco.engine.test.TestItem
import io.moco.engine.test.TestItemWrapper
import io.moco.engine.test.TestResult
import io.moco.engine.test.TestResultAggregator
import io.moco.utils.ClassLoaderUtil
import io.moco.utils.MoCoLogger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking


object MutationTestExecutor {
    private val clsLoader = ClassLoaderUtil.contextClsLoader
    lateinit var testMonitor: MutationTestMonitor

    val logger = MoCoLogger()

    fun introduceMutantThenExec(
        mutantIntroducer: MutantIntroducer,
        mutation: Mutation, mutatedClass: Mutant,
        tests: List<TestItemWrapper>,
    ): MutationTestResult {
        val mtr: MutationTestResult
        val t0 = System.currentTimeMillis()
        if (mutantIntroducer.introduce(mutation.mutationID.location.className, clsLoader, mutatedClass.byteArray)) {
            logger.debug("Introduce mutant in " + (System.currentTimeMillis() - t0) + " ms")
            logger.debug("Mutation at line ${mutation.lineOfCode}")
            logger.debug("Mutation operator: ${mutation.description}")
            logger.debug("Mutator ID: ${mutation.mutationID.mutatorID}")
            mtr = executeTestAndGetResult(tests.toMutableList(), mutation)
        } else {
            return MutationTestResult(0, MutationTestStatus.RUN_ERROR)

        }
        return mtr
    }

    private fun executeTestAndGetResult(
        tests: MutableList<TestItemWrapper>, mutation: Mutation
    ): MutationTestResult {
        var killed = false
        var numberOfExecutedTests = 0
        var finalStatus: MutationTestStatus
        var killedByTest: TestItem? = null
        try {
            runBlocking {
                var errorCount = 0
                for (test: TestItemWrapper? in tests) {
                    try {
                        numberOfExecutedTests += 1
                        test?.call()
                        // A mutant is killed if a test is failed
                        killed = checkIfMutantWasKilled(test?.testResultAggregator)
                        // Early exit when the mutant was killed
                        if (killed) {
                            // put the test that killed this mutant to top of the tests list
                            logger.debug("${mutation.mutationID.mutatorID} was killed")
                            killedByTest = test?.testItem
                            tests.remove(test)
                            tests.add(0, test!!)
                            break
                        }
                    } catch (e: Exception) {
                        if (e is TimeoutCancellationException) {
                            testMonitor.markTimeoutMutationType(mutation)
                            logger.debug("Cancel remaining test of this mutants because of timeout error")
                            break
                        }
                        errorCount += 1
                        if (errorCount > 3) {
                            logger.debug("Ignore this mutant because it causes a lot of errors")
                            break
                        }
                        logger.debug("Error while executing test ${test?.testItem?.desc?.name}")
                    }
                }

                finalStatus = if (killed) MutationTestStatus.KILLED else MutationTestStatus.SURVIVED
                if (finalStatus == MutationTestStatus.SURVIVED) {
                    if (numberOfExecutedTests == tests.size &&
                        tests.all { it.testResultAggregator.results.any { it1 -> it1.error != null } }
                    ) {
                        // A mutant is not erroneous if all tests ran against it have thrown errors
                        finalStatus = MutationTestStatus.RUN_ERROR
                    }
                }
                // Reset test result aggregator of test classes before moving on to the next mutant
                tests.map { it.testResultAggregator.results.clear() }
            }
            return MutationTestResult(numberOfExecutedTests, finalStatus, killedByTest.toString())
        } catch (ex: Exception) {
            return MutationTestResult(numberOfExecutedTests, MutationTestStatus.RUN_ERROR)
        }
    }

    private fun checkIfMutantWasKilled(tra: TestResultAggregator?): Boolean {
        logger.debug("Test ${tra?.results?.last()?.desc?.name} result: ${tra?.results?.last()?.state}")
        if (tra != null) {
            for (r: TestResult in tra.results) {
                if ((r.state == TestResult.TestState.FINISHED) && (r.error != null)) {
                    return true
                }
            }
        }
        return false
    }
}