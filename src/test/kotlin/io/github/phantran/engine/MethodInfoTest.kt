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

package io.github.phantran.engine

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe

class MethodInfoTest: AnnotationSpec() {

    @Test
    fun testMethodInfo() {
        val temp = MethodInfo(null, 1, "abc", "I")
        temp.enclosingClass shouldBe null
        temp.access shouldBe 1
        temp.name shouldBe "abc"
        temp.methodDescriptor shouldBe "I"
        temp.description shouldBe temp.enclosingClass?.name + "::" + temp.name
        temp.isGeneratedEnumMethod shouldBe false
        val mi1 = ClassInfo(5, 5, "a",
            "x", "java/lang/Enum", arrayOf("a"))
        val temp1 = MethodInfo(mi1, 1, "values", "I")
        val mi2 = ClassInfo(5, 5, "a",
            "x", "zxc", arrayOf("a"))
        temp1.isGeneratedEnumMethod shouldBe false
        val temp2 = MethodInfo(mi2, 1, "values", "I")
        temp2.isGeneratedEnumMethod shouldBe false
        val temp3 = MethodInfo(mi1, 1, "valueOf", "I")
        temp3.isGeneratedEnumMethod shouldBe false
        val temp4 = MethodInfo(mi1, 1, "<clinit>", "I")
        temp4.isGeneratedEnumMethod shouldBe true
    }
}