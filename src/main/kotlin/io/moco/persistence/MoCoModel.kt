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

import io.moco.utils.MoCoLogger


open class MoCoModel(
    open var entry: MutableMap<String, String> = mutableMapOf()
) {
    val logger = MoCoLogger()
    open val sourceName: String = ""

    open fun save() {
        // This method insert or update (if exists) a PersistentMutationResult record to its table
        H2Database().insertOrUpdateIfExist(sourceName, entry)
    }

    open fun getData(condition: String): List<MoCoModel> {
        // This method return a list of (PersistentMutationResult) recorded mutation test results
        // each record contains (location, killed or survived...)
        val con = H2Database.getConnection()
        val temp = H2Database().fetch(con, sourceName, condition)
        val res: MutableList<MoCoModel> = mutableListOf()
        while (temp?.next() == true) {
            val tempEntry: MutableMap<String, String> = mutableMapOf()
            for (key in entry.keys) {
                tempEntry[key] = temp.getString(key)
            }
            res.add(MoCoModel(tempEntry))
        }
        con.close()
        return res
    }

    open fun removeData(condition: String) {
        H2Database().delete(sourceName, condition)
    }

    companion object {
        fun saveMultipleEntries(sourceName: String, entries: List<MutableMap<String, String?>>) {
            // This method insert or update (if exists) a PersistentMutationResult record to its table
            H2Database().multipleInsertOrUpdateIfExist(sourceName, entries)
        }
    }
}