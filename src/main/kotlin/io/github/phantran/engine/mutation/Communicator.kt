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

import io.github.phantran.utils.DataStreamUtils
import java.io.DataOutputStream
import java.io.IOException
import java.io.Serializable


class Communicator(private val outputStream: DataOutputStream) {

    @Throws(IOException::class)
    @Synchronized
    fun registerToMainProcess(mutationID: MutationID?) {
        outputStream.writeByte(ResultsReceiverThread.register.toInt())
        if (mutationID != null) {
            DataStreamUtils.writeObject(outputStream, mutationID)
        }
        outputStream.flush()
    }

    @Throws(IOException::class)
    @Synchronized
    fun reportToMainProcess(
        mutation: Mutation?,
        mutationTestResult: MutationTestResult?
    ) {
        outputStream.writeByte(ResultsReceiverThread.report.toInt())
        if (mutation?.mutationID != null) {
            DataStreamUtils.writeObject(outputStream, mutation.mutationID)
        }
        if (mutation?.instructionsOrder != null) {
            DataStreamUtils.writeObject(outputStream, mutation.instructionsOrder as Serializable)
        }
        if (mutation != null) {
            DataStreamUtils.writeObject(outputStream, mutation.additionalInfo as Serializable)
        }
        if (mutationTestResult != null) {
            DataStreamUtils.writeObject(outputStream, mutationTestResult)
        }
        outputStream.flush()
    }

    @Throws(IOException::class)
    @Synchronized
    fun finishedMessageToMainProcess(exitCode: Int) {
        outputStream.writeByte(ResultsReceiverThread.finished.toInt())
        outputStream.writeInt(exitCode)
        outputStream.flush()
    }
}