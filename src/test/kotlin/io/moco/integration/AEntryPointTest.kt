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
import io.kotest.matchers.ints.shouldBeInRange
import io.mockk.unmockkAll
import io.moco.engine.Configuration
import io.moco.engine.MoCoEntryPoint
import io.moco.engine.operator.Operator
import io.moco.persistence.H2Database
import io.moco.utils.MoCoLogger
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Paths
import kotlin.math.roundToInt


class AEntryPointTest : AnnotationSpec() {

    private val baseDir = Paths.get("").toAbsolutePath().toString()
    private val buildRoot = "$baseDir/src/test/resources/test-artifacts/"
    private val codeRoot = "$baseDir/src/test/resources/test-artifacts/sources"
    private val testRoot = "$baseDir/src/test/resources/test-artifacts/tests"
    private val classpath =
        System.getProperty("java.class.path") + File.pathSeparator + codeRoot + File.pathSeparator + testRoot
    private val jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"

    @BeforeEach
    fun init() {
        unmockkAll()
        FileUtils.deleteDirectory(File(buildRoot + "moco"))
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
        FileUtils.deleteDirectory(File(buildRoot + "moco"))
    }

    @Test
    fun testEntryPoint() {
        try {
            val excluded = ""
            val fOpNames = Operator.supportedOperatorNames.filter { !excluded.contains(it) }
            val configuration = Configuration(
                baseDir,
                System.currentTimeMillis().toString(),
                buildRoot,
                codeRoot,
                testRoot,
                codeRoot,
                testRoot,
                "$buildRoot${File.separator}moco",
                "dev.Hihi",
                "",
                "dev.HihiTest",
                "io/moco/integration/",
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
                0,
                true,
                debugEnabled = false,
                verbose = false,
                2,
                noLogAtAll = true,
                enableMetrics = true,
                mocoPluginVersion = "1.0-SNAPSHOT"
            )
            Configuration.currentConfig = configuration
            MoCoLogger.useKotlinLog()
            MoCoLogger.noLogAtAll = true
            MoCoLogger.debugEnabled = Configuration.currentConfig!!.debugEnabled
            H2Database.initPool(
                url = "jdbc:h2:file:${Configuration.currentConfig?.mocoBuildPath}" +
                        "${File.separator}/persistence/moco;mode=MySQL",
                user = "moco",
                password = "moco",
            )
            H2Database().initDBTablesIfNotExists()
            MoCoEntryPoint(configuration).execute()
            MoCoEntryPoint.runScore.roundToInt() shouldBeInRange IntRange(50, 60)
        } finally {
            H2Database.shutDownDB()
        }
    }
}