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
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.moco.persistence.H2Database
import io.moco.persistence.MutationStorage
import io.moco.utils.GitProcessor
import io.moco.utils.MoCoLogger
import java.nio.file.Paths


class MetricsTest: AnnotationSpec() {

    @BeforeEach
    fun init() {
    }

    @Test
    fun testCalculateRunCoverage() {
        val temp = mockkClass(MutationStorage::class)
        every { temp.entries } returns mutableMapOf("a" to mutableListOf(mapOf("result" to "run_error"),
                                                                        mapOf("result" to "killed"),
                                                                         mapOf("result" to "survived")),
            "b" to mutableListOf(mapOf("result" to "run_error"),
                mapOf("result" to "killed"),
                mapOf("result" to "survived"))
        )
        val m = Metrics(temp)
        m.calculateRunCoverage(temp) shouldBe 50.0
    }
}