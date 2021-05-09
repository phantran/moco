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

package io.github.phantran.persistence

import io.github.phantran.engine.ClassName
import io.github.phantran.engine.MethodName
import io.github.phantran.engine.mutation.MutatedMethodLocation
import io.github.phantran.engine.mutation.Mutation
import io.github.phantran.engine.mutation.MutationID
import io.github.phantran.engine.preprocessing.PreprocessStorage
import org.json.JSONObject


data class PersistentMutationResult(
    override var entry: MutableMap<String, String> =
        mutableMapOf(
            "className" to "", "methodName" to "", "methodDesc" to "",
            "instructionIndices" to "", "mutationOperatorName" to "", "mutatorID" to "",
            "fileName" to "", "loc" to "", "mutationDescription" to "", "instructionOrder" to "",
            "additionalInfo" to "", "result" to "", "killedByTest" to "", "uniqueID" to "",
        ),
) : MoCoModel() {

    override val sourceName = "PersistentMutationResult"

    private fun createEntry(
        mutationDetails: Mutation,
        mutationID: MutationID,
        item: MutableMap<String, Any?>
    ): MutableMap<String, String?> {
        return mutableMapOf(
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
            "killedByTest" to item["killedByTest"] as String,
            "uniqueID" to item["uniqueID"].toString(),
        )
    }

    fun saveMutationResult(
        data: MutationStorage,
        clsChangedByGit: List<String>?,
        preprocessedStorage: PreprocessStorage
    ) {
        val entries: MutableSet<MutableMap<String, String?>> = mutableSetOf()

        for ((cls, value) in data.entries) {
            if (clsChangedByGit?.isEmpty() == true || clsChangedByGit?.contains(cls) == true) {
                // If git diff is an empty list -> first run or DB has been reset -> update PersistentMutationResult normally
                // If a class has been changed according to Git diff -> we remove all mutation results of this
                // class in PersistentMutationResult table
                // Since it is a changed class, we can make sure that all mutations of this class have been
                // generated and tested with all relevant test cases in this run
                this.removeData("className = '$cls'")
                for (item in value) {
                    // Since we have generated and checked all mutations of this class
                    // we can proceed and update PersistentMutationResult with all
                    // entries of mutationStorage for this class
                    if (item["result"] as String != "run_error") {
                        val mutationDetails = item["mutationDetails"] as Mutation
                        val mutationID = mutationDetails.mutationID
                        entries.add(createEntry(mutationDetails, mutationID, item))
                    }
                }
            } else {
                // this case is for an unchanged class, it is not in the list of changed class by git
                // so we deal with it differently, we check for killed mutations of this class (from DB)
                // and the corresponding tests that killed them. If those tests were also executed
                // in this run, then we update those mutation statuses. Otherwise, we keep their statuses as "killed"
                val className = cls.replace(".", "/")
                val existingData = PersistentMutationResult().getData("className = '$className'")

                for (item in value) {
                    // In case of updating a row result from killed to survived
                    // We need to make sure the test case that killed this mutation before is also in
                    // the executed tests list, if that test is not in the list, we don't change the status
                    // from killed to survived (skip)
                    if (item["result"] == "survived") {
                        val preprocessInfoOfCls = preprocessedStorage.classRecord.find {
                            it.classUnderTestName.replace("/", ".") == cls
                        }
                        val lineTestsMapping = preprocessInfoOfCls?.coveredLines!!

                        if (existingData.any {
                                it.entry["uniqueID"]!!.toInt() == item["uniqueID"] &&
                                        it.entry["result"] == "killed" &&
                                        // mutationStorage of this run does not execute the test case that killed this mutant before
                                        !lineTestsMapping.any { it1 ->
                                            it1.key.toString() == it.entry["loc"] &&
                                                    it1.value.any { it2 ->
                                                        it2.name == it.entry["killedByTest"]
                                                    }
                                        }
                            }) {
                            continue
                        }
                    }
                    // Other cases -> killed to killed, survived to killed, survived to survived can be updated
                    // without any problem
                    if (item["result"] as String != "run_error") {
                        val mutationDetails = item["mutationDetails"] as Mutation
                        val mutationID = mutationDetails.mutationID
                        entries.add(createEntry(mutationDetails, mutationID, item))
                    }
                }
            }
        }
        saveMultipleEntries(sourceName, entries.toList())
    }


    @Suppress("UNCHECKED_CAST")
    fun getAllData(killedIncluded: Boolean = false): MutableMap<String, MutableSet<MutableMap<String, Any?>>> {
        // This method will be used to retrieve all persisted mutation results in case
        // moco.json has not been created or it has been deleted
        val retrieved = if (!killedIncluded) this.getData("result = 'survived'") else this.getData("")
        val res: MutableMap<String, MutableSet<MutableMap<String, Any?>>> = mutableMapOf()
        retrieved.map {
            val mutation = Mutation(
                MutationID(
                    MutatedMethodLocation(
                        ClassName(it.entry["className"]!!),
                        MethodName(it.entry["methodName"]!!),
                        it.entry["methodDesc"]!!
                    ),
                    it.entry["instructionIndices"]!!.split(",").map { it1 -> it1.toInt() },
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
                "killedByTest" to it.entry["killedByTest"]!!,
                "uniqueID" to it.entry["uniqueID"]!!.toInt()
            )
            val clsName = it.entry["className"]?.replace("/", ".")
            if (clsName in res) {
                res[clsName]!!.add(mutationInfo)
            } else {
                res[clsName!!] = mutableSetOf(mutationInfo)
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
                    "uniqueID INT NOT NULL," +
                    "killedByTest VARCHAR(255)," +
                    "updatedAt TIMESTAMP NOT NULL DEFAULT NOW() ON UPDATE NOW()," +
                    "UNIQUE KEY uniqueMutation (uniqueID)"
    }
}
