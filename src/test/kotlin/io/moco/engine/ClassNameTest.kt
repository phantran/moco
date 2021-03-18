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


class ClassNameTest: AnnotationSpec()  {

    @BeforeEach
    fun init() {
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun testJavaName() {
        val t = ClassName("org/example")
        t.getJavaName() shouldBe "org.example"
    }

    @Test
    fun testFromString() {
        ClassName.fromString("org.example") shouldBe ClassName("org/example")
    }

    @Test
    fun testClsNameToClass() {
        ClassName.clsNameToClass(ClassName("io.moco.engine.ClassInfo")) shouldBe ClassInfo::class.java
        ClassName.clsNameToClass(ClassName("io.moco.engine.ClassInfo1")) shouldBe null
    }
}