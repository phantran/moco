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
import io.mockk.unmockkAll
import io.moco.engine.operator.Operator
import io.moco.persistence.H2Database
import io.moco.utils.MoCoLogger
import org.junit.Test
import java.io.File
import java.nio.file.Paths
import org.apache.commons.io.FileUtils;


class MoCoEntryPointTest: AnnotationSpec() {

    private val baseDir = Paths.get("").toAbsolutePath().toString()
    private val buildRoot = baseDir + "/src/test/resources/test-artifacts/"
    private val codeRoot = baseDir + "/src/test/resources/test-artifacts/sources"
    private val testRoot = baseDir + "/src/test/resources/test-artifacts/tests"
    private val classpath = System.getProperty("java.class.path") + File.pathSeparator + codeRoot + File.pathSeparator + testRoot
    private val jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"

    @AfterAll
    fun cleanUp() {
        unmockkAll()
        FileUtils.deleteDirectory( File(buildRoot + "moco"));
    }

    @Test
    fun testEntryPoint() {


        val excluded = ""
        val fOpNames = Operator.supportedOperatorNames.filter { !excluded.contains(it) }

        val configuration = Configuration(
            buildRoot,
            codeRoot,
            testRoot,
            "$buildRoot${File.separator}moco",
            "dev.Hihi",
            "exclusion",
            "dev.Hihi",
            "exclusion",
            classpath,
            jvm,
            "preprocess",
            "mutation",
            excluded,
            fOpNames,
            buildRoot,
            listOf(),
            "dev",
            false,
            "1000",
            5,
            debugEnabled = false,
            verbose = false,
            2
        )
        Configuration.currentConfig = configuration
        MoCoLogger.noLogAtAll = true
        MoCoLogger.useKotlinLog()
        MoCoLogger.debugEnabled = Configuration.currentConfig!!.debugEnabled
        H2Database.initPool(
            url = "jdbc:h2:file:${Configuration.currentConfig?.mocoBuildPath}" +
                    "${File.separator}/persistence/moco;mode=MySQL",
            user = "moco",
            password = "moco",
        )
        H2Database().initDBTablesIfNotExists()
        MoCoEntryPoint(configuration).execute()
        H2Database.shutDownDB()
    }
}