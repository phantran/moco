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

package io.moco.integration

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldNotBe
import io.mockk.unmockkAll
import io.moco.engine.Codebase
import io.moco.engine.Configuration
import io.moco.engine.operator.Operator
import io.moco.engine.preprocessing.Preprocessor
import io.moco.engine.preprocessing.PreprocessorTracker
import io.moco.engine.preprocessing.PreprocessorWorker
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Paths

class PreprocessWorkerTest: AnnotationSpec() {

    private val baseDir = Paths.get("").toAbsolutePath().toString()
    private val buildRoot = "$baseDir/src/test/resources/test-artifacts/"
    private val codeRoot = "$baseDir/src/test/resources/test-artifacts/sources"
    private val testRoot = "$baseDir/src/test/resources/test-artifacts/tests"
    private val classpath = System.getProperty("java.class.path") + File.pathSeparator + codeRoot + File.pathSeparator + testRoot
    private val jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
    private val excluded = ""
    private val fOpNames = Operator.supportedOperatorNames.filter { !excluded.contains(it) }
    private val configuration = Configuration(
        buildRoot,
        codeRoot,
        testRoot,
        "$buildRoot${File.separator}moco",
        "dev.Hihi",
        "",
        "dev.HihiTest",
        "",
        classpath,
        jvm,
        "preprocess",
        "mutation",
        excluded,
        fOpNames,
        buildRoot,
        listOf(),
        "dev",
        "m0c0-maven-plugin",
        false,
        "200",
        5,
        debugEnabled = true,
        verbose = true,
        2,
        noLogAtAll = true,
        mocoPluginVersion = "1.0-SNAPSHOT"
    )

    @AfterAll
    fun cleanUp() {
        unmockkAll()
        FileUtils.deleteDirectory(File(buildRoot + "moco"));
        Configuration.currentConfig = null
    }

    @Test
    fun testMain() {
        val args = arrayOf("0", *configuration.getPreprocessProcessArgs().toTypedArray(), "", "", "false")
        PreprocessorWorker.prepareWorker(args)
        val analysedCodeBase = Codebase(
            PreprocessorWorker.codeRoot, PreprocessorWorker.testRoot, PreprocessorWorker.excludedSourceClasses,
            PreprocessorWorker.excludedSourceFolders, PreprocessorWorker.excludedTestClasses, PreprocessorWorker.excludedTestFolders
        )
        val relevantTests = PreprocessorWorker.getRelevantTests(
            PreprocessorWorker.filteredClsByGitCommit,
            analysedCodeBase,
            PreprocessorWorker.recordedTestMapping
        )
        Preprocessor(relevantTests).preprocessing(PreprocessorWorker.isRerun, PreprocessorWorker.jsonConverter)
        PreprocessorTracker.getPreprocessResults() shouldNotBe null

    }
}