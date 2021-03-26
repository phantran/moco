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
import io.kotest.matchers.shouldNotBe
import io.mockk.unmockkAll
import java.nio.file.Paths

class CodebaseTest : AnnotationSpec() {

    @BeforeEach
    fun init() {
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun testCodeBase() {
        val excludedSourceClasses = mutableListOf("")
        val excludedSourceFolders = mutableListOf("io/moco/engine/mutator")
        val excludedTestClasses = mutableListOf("")
        val excludedTestFolders = mutableListOf("")
        val codePath = Paths.get("").toAbsolutePath().toString() + "/target/classes/io/moco/engine"
        val testPath = Paths.get("").toAbsolutePath().toString() + "/target/test-classes/io/moco/engine"
        val codeBase = Codebase(
            codePath, testPath, codePath, testPath, excludedSourceClasses, excludedSourceFolders,
            excludedTestClasses, excludedTestFolders
        )
        codeBase.sourceClassNames.size shouldNotBe 0
        codeBase.toString() shouldNotBe ""
    }
}