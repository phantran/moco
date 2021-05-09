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
import io.github.phantran.engine.ClassInfo
import io.github.phantran.engine.ClassName
import io.github.phantran.utils.DataStreamUtils
import java.io.DataOutputStream
import java.io.OutputStream
import java.net.Socket

class CommunicatorTest: AnnotationSpec()  {

    @BeforeEach
    fun init() {
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun testRegisterToMainProcess() {
        val ot = mockkClass(DataOutputStream::class)
        every { ot.writeByte(1) } answers {  }
        every { ot.flush() } answers {  }
        val temp = Communicator(ot)
        temp.registerToMainProcess(null)
        val temp1 = mockkClass(MutationID::class)
        mockkObject(DataStreamUtils)
        every { DataStreamUtils.writeObject(any(), any()) } answers { }
        temp.registerToMainProcess(temp1)
    }

    @Test
    fun testReportToMainProcess() {
        val ot = mockkClass(DataOutputStream::class)
        every { ot.writeByte(2) } answers {  }
        every { ot.flush() } answers {  }
        val temp = Communicator(ot)
        temp.reportToMainProcess(null, null)
    }

    @Test
    fun testFinishedMessageToMainProcess() {
        val ot = mockkClass(DataOutputStream::class)
        every { ot.writeByte(4) } answers {  }
        every { ot.writeInt(100) } answers {  }
        every { ot.flush() } answers {  }
        val temp = Communicator(ot)
        temp.finishedMessageToMainProcess(100)
    }
}