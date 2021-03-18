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

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import io.moco.engine.operator.Operator
import java.io.File

class ConfigurationTest: AnnotationSpec() {

    @BeforeEach
    fun init() {
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun testGetPreprocessProcessArgs() {
        val buildRoot =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target"
        val codeRoot =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/classes"
        val testRoot =
            "/Users/phantran/Study/Passau/Thesis/Moco/m0c0-maven-plugin/target/test-classes"
        val classpath = System.getProperty("java.class.path") + File.pathSeparator + codeRoot + File.pathSeparator + testRoot
        val jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
        val excluded = ""
        val fOpNames = Operator.supportedOperatorNames.filter { !excluded.contains(it) }

        val configuration = Configuration(
            buildRoot,
            codeRoot,
            testRoot,
            "$buildRoot${File.separator}moco",
            "io/moco/MoCo",
            "io/moco/engine, io/moco/dev/main, io/moco/persistence, io/moco/utils",
            "io/moco/dev/Abc",
            "io/moco/engine, io/moco/dev/DEV, io/moco/persistence, io/moco/utils",
            classpath,
            jvm,
            "preprocess",
            "mutation",
            excluded,
            fOpNames,
            "",
            listOf(),
            "io.moco",
            false,
            "1000",
            5,
            debugEnabled = true,
            verbose = true,
            2
        )
        configuration.buildRoot shouldBe buildRoot
        configuration.codeRoot shouldBe codeRoot
        configuration.testRoot shouldBe testRoot
        configuration.mocoBuildPath shouldBe "$buildRoot${File.separator}moco"
        configuration.excludedSourceClasses shouldBe "io/moco/MoCo"
        configuration.excludedSourceFolders shouldBe "io/moco/engine, io/moco/dev/main, io/moco/persistence, io/moco/utils"
        configuration.excludedTestClasses shouldBe "io/moco/dev/Abc"
        configuration.excludedTestFolders shouldBe "io/moco/engine, io/moco/dev/DEV, io/moco/persistence, io/moco/utils"
        configuration.classPath shouldBe classpath
        configuration.jvm shouldBe jvm
        configuration.preprocessResultsFolder shouldBe "preprocess"
        configuration.mutationResultsFolder shouldBe  "mutation"
        configuration.excludedMuOpNames shouldBe excluded
        configuration.fOpNames shouldBe fOpNames
        configuration.baseDir shouldBe ""
        configuration.compileSourceRoots shouldBe listOf()
        configuration.artifactId shouldBe "io.moco"
        configuration.gitMode shouldBe false
        configuration.preprocessTestTimeout shouldBe "1000"
        configuration.mutationPerClass shouldBe 5
        configuration.debugEnabled shouldBe  true
        configuration.verbose shouldBe true
        configuration.numberOfThreads shouldBe 2
        configuration.getPreprocessProcessArgs()[0] shouldBe "$buildRoot${File.separator}moco"
        Configuration.currentConfig shouldBe null
    }
}