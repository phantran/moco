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

package io.moco.engine.test

import io.moco.engine.ClassName
import io.moco.utils.MoCoLogger
import junit.framework.TestCase
import junit.framework.TestSuite
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.SelectPackages
import org.junit.runners.Suite


open class TestItem(
    val cls: Class<*>,
    val testIdentifier: Any,
    var executionTime: Long = -1
) {
    open val desc: Description = Description("", this.cls.name)
    val logger = MoCoLogger()

    enum class TestFramework {
        JUNIT3, JUNIT4, JUNIT5, TESTNG, UNKNOWN
    }

    open suspend fun execute(tra: TestResultAggregator, timeOut: Long = -1) {}

    companion object {
        fun testsInfoToTestItems(testsInfo: MutableSet<SerializableTestInfo>?): List<TestItem> {
            // convert from test classes to test items so it can be executed
            val res: MutableList<TestItem> = mutableListOf()
            testsInfo?.mapNotNull {
                val clsName = ClassName.fromString(it.cls)
                if (it.executionTime >= 0) {
                    // execution time < 0 means error tests in preprocessing phase
                    val testClass = ClassName.clsNameToClass(clsName)
                    if (it.testIdentifier.isNotEmpty()) {
                    when (checkTestFramework(testClass as Class<*>)) {
                            TestFramework.JUNIT3 -> res.add(JUnit34TestItem(testClass, it.testIdentifier, true, it.executionTime))
                            TestFramework.JUNIT4 -> res.add(JUnit34TestItem(testClass, it.testIdentifier, false, it.executionTime))
                            TestFramework.JUNIT5 -> {
                                val testItem = JUnit5TestItem.getTestItem(testClass, it.testIdentifier, it.executionTime)
                                if (testItem != null) res.add(testItem)
                                else null
                            }
                            TestFramework.TESTNG -> res.add(TestNGTestItem(testClass, it.testIdentifier, it.executionTime))

                        else -> null
                    }
                    } else null
                } else null
            }
            return res
        }

        fun testClassesToTestItems(
            testClassNames: List<ClassName>,
        ): List<TestItem> {
            // convert from test classes to test items so it can be executed
            var testClasses = testClassNames.mapNotNull {
                ClassName.clsNameToClass(it)
            }
            testClasses = testClasses.filter { isNotTestSuite(it, checkTestFramework(it)) }
            val res: MutableList<TestItem> = mutableListOf()
            for (item in testClasses) {
                val typeFramework = checkTestFramework(item)
                if (isNotTestSuite(item, typeFramework)) {
                    when (typeFramework) {
                        TestFramework.JUNIT3 -> JUnit34TestItem.getTests(res, true, item)
                        TestFramework.JUNIT4 -> JUnit34TestItem.getTests(res, false, item)
                        TestFramework.JUNIT5 -> JUnit5TestItem.getTests(res, item)
                        TestFramework.TESTNG -> TestNGTestItem.getTests(res, item)
                        else -> {
                        }
                    }
                }
            }
            return res
        }

        private fun isNotTestSuite(cls: Class<*>, testFramework: TestFramework): Boolean {
            // Ignore test suite class since all normal test classes are recorded.
            when (testFramework) {
                TestFramework.JUNIT3 -> {
                    if (cls.superclass != TestSuite::class.java) {
                        return true
                    }
                }
                TestFramework.JUNIT4 -> {
                    if (cls.getAnnotation(Suite.SuiteClasses::class.java) == null) {
                        return true
                    }
                }
                TestFramework.JUNIT5 -> {
                    if (cls.getAnnotation(SelectClasses::class.java) == null &&
                        cls.getAnnotation(SelectPackages::class.java) == null
                    ) {
                        return true
                    }
                }
                TestFramework.TESTNG -> return true
                else -> return false
            }
            return false
        }


        private fun checkTestFramework(cls: Class<*>): TestFramework {
            // junit3 extends test case or testsuite class, junit4 has RunWith or Test annotation
            if (cls.declaredMethods.any {
                    it.annotations.any { it1 ->
                        it1.annotationClass.java == org.junit.Test::class.java
                    }
                }) {
                return TestFramework.JUNIT4
            } else if (cls.declaredMethods.any {
                    it.annotations.any { it1 ->
                        it1.annotationClass.java == org.junit.jupiter.api.Test::class.java
                    }
                }) {
                return TestFramework.JUNIT5
            } else if (cls.superclass == TestCase::class.java) {
                return TestFramework.JUNIT3
            } else if (cls.declaredMethods.any {
                    it.annotations.any { it1 ->
                        it1.annotationClass.java == org.testng.annotations.Test::class.java
                    }
                }) {
                return TestFramework.TESTNG
            }
            return TestFramework.UNKNOWN
        }
    }
}