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

package io.github.phantran.engine.mutation

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.github.phantran.engine.tracker.MutatedMethodTracker
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor

class InstructionVisitorTest : AnnotationSpec() {

    @BeforeEach
    fun init() {
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun testVisitInvokeDynamicInsn() {
        val mockMv = mockkClass(MethodVisitor::class)
        val mt = mockkClass(MutatedMethodTracker::class)
        every { mt setProperty "instructionIndex" value less(5)} just runs
        every { mt.instructionIndex } returns 1
        val temp = InstructionVisitor(mockMv, mt)
        val temp1 = mockkClass(Handle::class)
        every { mockMv.visitInvokeDynamicInsn(any(), any(), any()) } answers { }
        temp.visitInvokeDynamicInsn("a", "a", temp1)
        mt.instructionIndex shouldBe 1
    }

    @Test
    fun testVisitMultiANewArrayInsn() {
        val mockMv = mockkClass(MethodVisitor::class)
        val mt = mockkClass(MutatedMethodTracker::class)
        every { mt setProperty "instructionIndex" value less(5)} just runs
        every { mt.instructionIndex } returns 1
        val temp = InstructionVisitor(mockMv, mt)
        every { mockMv.visitMultiANewArrayInsn(any(), any()) } answers { }
        temp.visitMultiANewArrayInsn("a", 5)
        mt.instructionIndex shouldBe 1
    }

    @Test
    fun testVisitLookupSwitchInsn() {
        val mockMv = mockkClass(MethodVisitor::class)
        val mt = mockkClass(MutatedMethodTracker::class)
        every { mt setProperty "instructionIndex" value less(5)} just runs
        every { mt.instructionIndex } returns 1
        val temp = InstructionVisitor(mockMv, mt)
        every { mockMv.visitLookupSwitchInsn(any(), any(), any()) } answers { }
        temp.visitLookupSwitchInsn(Label(), IntArray(5), arrayOf())
        mt.instructionIndex shouldBe 1
    }

    @Test
    fun testVisitTableSwitchInsn() {
        val mockMv = mockkClass(MethodVisitor::class)
        val mt = mockkClass(MutatedMethodTracker::class)
        every { mt setProperty "instructionIndex" value less(5)} just runs
        every { mt.instructionIndex } returns 1
        val temp = InstructionVisitor(mockMv, mt)
        every { mockMv.visitTableSwitchInsn(any(), any(), any()) } answers { }
        temp.visitTableSwitchInsn(1, 1, Label())
        mt.instructionIndex shouldBe 1
    }
}