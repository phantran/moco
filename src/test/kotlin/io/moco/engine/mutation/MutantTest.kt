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

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockkClass
import io.mockk.unmockkAll

class MutantTest: AnnotationSpec() {

    @BeforeEach
    fun init() {
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun testEquals() {
        val temp = mockkClass(Mutation::class)
        val temp1 = ByteArray(1)
        val temp2 = Mutant(temp, temp1)
        val temp3 = Mutant(temp, temp1)
        (temp2 == temp3) shouldBe true
    }


    @Test
    fun testHashcode() {
        val temp = mockkClass(Mutation::class)
        val temp1 = ByteArray(1)
        val temp2 = Mutant(temp, temp1)
        temp2.hashCode() shouldNotBe 0
    }
}