package io.moco.engine.test

import io.moco.engine.ClassName
import io.moco.engine.preprocessing.PreprocessClassResult
import io.moco.engine.preprocessing.PreprocessStorage
import org.junit.internal.runners.ErrorReportingRunner
import org.junit.runner.RunWith
import org.junit.runner.Runner
import org.junit.runners.Parameterized
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses


class RelatedTestRetriever(buildRoot: String) {

    private val store: PreprocessStorage = PreprocessStorage.getStoredPreprocessStorage(buildRoot)

    fun retrieveRelatedTest(cut: ClassName): List<ClassName> {
        for (item: PreprocessClassResult in store.classRecord) {
            if (cut.name == item.classUnderTestName) {
                return item.testClasses.map { ClassName(it.first.replace(".", "/")) }
            }
        }
        return listOf()
    }


    fun retrieveTestsOfClasses(clsList: List<Class<*>>): List<TestItem?> {
        val testItems: MutableList<TestItem?> = mutableListOf()
        for (cls in clsList) {
            val retrievedTC: MutableList<TestItem?> = mutableListOf()
            val visitedClasses = mutableSetOf<Class<*>>()
            findTestItems(retrievedTC, visitedClasses, cls)
            testItems.addAll(retrievedTC)
        }
        return testItems
    }

    private fun findTestItems(
        retrievedTC: MutableList<TestItem?>, visitedClasses: MutableSet<Class<*>>, suiteClass: Class<*>
    ) {
        visitedClasses.add(suiteClass)
        // collect classes belong to a test suite (if test suite is detected)
        // and recursively call this function to get all test cases
        val tcs: List<Class<*>> = findTestSuites(suiteClass)
        for (tc in tcs) {
            if (!visitedClasses.contains(tc)) {
                findTestItems(retrievedTC, visitedClasses, tc)
            }
        }
        val testsInThisClass: List<TestItem?> = findTestItems(suiteClass)
        if (testsInThisClass.isNotEmpty()) {
            retrievedTC.addAll(testsInThisClass)
        }
    }

    fun findTestSuites(cls: Class<*>): List<Class<*>> {
        val temp: SuiteClasses = cls.getAnnotation(SuiteClasses::class.java)
        val res: MutableList<Class<*>> = mutableListOf()
        for (item in temp.value) {
            res.add(item::class.java)
        }
        return if (hasSuitableRunner(cls)) {
            return res
        } else {
            emptyList()
        }
    }

    private fun hasSuitableRunner(cls: Class<*>): Boolean {
        val runWith = cls.getAnnotation(RunWith::class.java)
        return if (runWith != null) {
            runWith.value == Suite::class.java
        } else false
    }


    fun findTestItems(cls: Class<*>): List<TestItem?> {
        return emptyList()
    }


    private fun notARunnableTest(
        runner: Runner?,
        className: String
    ): Boolean {
        return try {
            (runner == null
                    || runner.javaClass.isAssignableFrom(ErrorReportingRunner::class.java)
                    || Parameterized::class.java.isAssignableFrom(runner.javaClass)
                    || ((runner.description.children.isNotEmpty() && runner.description.children[0].className.startsWith(
                "junit.framework.TestSuite"
            )))
                    || ((runner.javaClass.name == "org.junit.internal.runners.SuiteMethod") && runner.description.className != className)
                    )
        } catch (ex: RuntimeException) {
            true
        }
    }
}