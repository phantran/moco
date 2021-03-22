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

package io.moco.persistence

import io.moco.engine.ClassName
import io.moco.engine.MethodName
import io.moco.engine.mutation.MutatedMethodLocation
import io.moco.engine.mutation.Mutation
import io.moco.engine.mutation.MutationID
import org.json.JSONObject


data class PersistentMutationResult(
    override var entry: MutableMap<String, String> =
        mutableMapOf(
            "className" to "", "methodName" to "", "methodDesc" to "",
            "instructionIndices" to "", "mutationOperatorName" to "", "mutatorID" to "",
            "fileName" to "", "loc" to "", "mutationDescription" to "", "instructionOrder" to "",
            "additionalInfo" to "", "result" to "", "uniqueID" to "",
        ),
) : MoCoModel() {

    override val sourceName = "PersistentMutationResult"

    fun saveMutationResult(data: MutationStorage) {
        val entries: MutableSet<MutableMap<String, String?>> = mutableSetOf()
        for ((cls, value) in data.entries) {
            this.removeData("className = '$cls'")
            for (item in value) {
                if (item["result"] as String != "run_error") {
                    val mutationDetails = item["mutationDetails"] as Mutation
                    val mutationID = mutationDetails.mutationID
                    entries.add(
                        mutableMapOf(
                            "className" to mutationDetails.mutationID.location.className?.name,
                            "methodName" to mutationDetails.mutationID.location.methodName.name,
                            "methodDesc" to mutationDetails.mutationID.location.methodDesc,
                            "instructionIndices" to mutationID.instructionIndices!!.joinToString(","),
                            "mutationOperatorName" to mutationID.operatorName,
                            "mutatorID" to mutationID.mutatorID,
                            "fileName" to mutationDetails.fileName,
                            "loc" to mutationDetails.lineOfCode.toString(),
                            "mutationDescription" to mutationDetails.description,
                            "instructionOrder" to mutationDetails.instructionsOrder.joinToString(","),
                            "additionalInfo" to JSONObject(mutationDetails.additionalInfo).toString(),
                            "result" to item["result"] as String,
                            "uniqueID" to item["uniqueID"].toString(),
                        )
                    )
                }
            }
        }
        saveMultipleEntries(sourceName, entries.toList())
    }

    @Suppress("UNCHECKED_CAST")
    fun getAllData(): MutableMap<String, MutableSet<Map<String, Any?>>> {
        // This method will be used to retrieve all persisted mutation results in case
        // moco.json has not been created or it has been deleted
        val retrieved = this.getData("result = 'survived'")
        val res: MutableMap<String, MutableSet<Map<String, Any?>>> = mutableMapOf()
        retrieved.map {
            val mutation = Mutation(
                MutationID(
                    MutatedMethodLocation(
                    ClassName(it.entry["className"]!!),
                    MethodName(it.entry["methodName"]!!),
                    it.entry["methodDesc"]!!),
                    it.entry["instructionIndices"]!!.split(",").map{it1 -> it1.toInt()},
                    it.entry["mutationOperatorName"]!!,
                    it.entry["mutatorID"]!!,
                ),
                it.entry["fileName"]!!,
                it.entry["loc"]!!.toInt(),
                it.entry["mutationDescription"]!!,
                mutableSetOf(),
                it.entry["instructionOrder"]!!.split(",").toMutableList(),
                JSONObject(it.entry["additionalInfo"]!!).toMap() as MutableMap<String, String>,
                )
            val mutationInfo: MutableMap<String, Any?> = mutableMapOf(
                "mutationDetails" to mutation,
                "result" to it.entry["result"]!!,
                "uniqueID" to it.entry["uniqueID"]!!
            )
            if (it.entry["className"] in res) {
                res[it.entry["className"]]!!.add(mutationInfo)
            } else {
                res[it.entry["className"]!!] = mutableSetOf(mutationInfo)
            }
        }
        return res
    }

    companion object {
        const val schema: String =
            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                    "className VARCHAR(255)," +
                    "methodName VARCHAR(255)," +
                    "methodDesc VARCHAR(255)," +
                    "instructionIndices VARCHAR(255)," +
                    "mutationOperatorName VARCHAR(255)," +
                    "mutatorID VARCHAR(255)," +
                    "fileName VARCHAR(255)," +
                    "loc INT(8) UNSIGNED NOT NULL," +
                    "mutationDescription VARCHAR(255)," +
                    "instructionOrder VARCHAR(255)," +
                    "additionalInfo VARCHAR(255)," +
                    "result VARCHAR(255)," +
                    "uniqueID VARCHAR(255)," +
                    "updatedAt TIMESTAMP NOT NULL DEFAULT NOW() ON UPDATE NOW()," +
                    "UNIQUE KEY uniqueMutation (uniqueID)"
    }
}
