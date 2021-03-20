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
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.SelectPackages
import org.junit.runners.Suite

open class TestItem(val cls: Class<*>, var executionTime: Long = -1) {
    open val desc: Description = Description(this.cls.name, this.cls.name)
    val logger = MoCoLogger()

    enum class TestFramework {
        JUNIT34, JUNIT5, TESTNG, UNKNOWN
    }

    open suspend fun execute(tra: TestResultAggregator, timeOut: Long = -1) {}

    companion object {
        fun testClassesToTestItems(
            testClassNames: List<ClassName>,
            testsExecutionTime: MutableMap<String, Long>? = null
        ): List<TestItem> {
            // convert from test classes to test items so it can be executed
            var testClasses = testClassNames.mapNotNull { ClassName.clsNameToClass(it) }
            testClasses = testClasses.filter { isNotTestSuite(it, checkTestFramework(it)) }
            val res: MutableList<TestItem> = mutableListOf()
            for (item in testClasses) {
                val typeFramework = checkTestFramework(item)
                if (isNotTestSuite(item, typeFramework)) {
                    var allowedTime = -1L
                    if (!testsExecutionTime.isNullOrEmpty()) {
                        allowedTime = if (testsExecutionTime[item.name] != null) testsExecutionTime[item.name]!! else -1
                    }
                    when (typeFramework) {
                        TestFramework.JUNIT34 -> res.add(JUnit34TestItem(item, executionTime = allowedTime))
                        TestFramework.JUNIT5 -> Junit5TestItem.getTests(res, item, executionTime = allowedTime)
                        TestFramework.TESTNG -> res.add(TestNGTestItem(item, executionTime = allowedTime))
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
                TestFramework.JUNIT34 -> {
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
            if (cls.superclass == TestCase::class.java ||
                cls.declaredMethods.any {
                    it.annotations.any { it1 ->
                        it1.annotationClass.java == org.junit.Test::class.java
                    }
                }
            ) {
                return TestFramework.JUNIT34
            } else if (cls.declaredMethods.any {
                    it.annotations.any { it1 ->
                        it1.annotationClass.java == org.junit.jupiter.api.Test::class.java
                    }
                }) {
                return TestFramework.JUNIT5
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