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
import kotlin.jvm.Throws


open class MoCoModel(
    open var entry: MutableMap<String, String> = mutableMapOf()
) {
    val logger = MoCoLogger()
    open val sourceName: String = ""

    open fun save() {
        H2Database().insertOrUpdateIfExist(sourceName, entry)
    }

    @Throws(Exception::class)
    open fun getData(condition: String): List<MoCoModel> {
        // This method return a list of MoCoModel results
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

    fun isEmptyTable(): Boolean {
        val con = H2Database.getConnection()
        val temp = H2Database().executeQuery(con, "select exists (select 1 from $sourceName) AS Output;")
        while (temp?.next() == true) {
            val res = temp.getBoolean("Output")
            if (!res) return true
        }
        return false
    }

    override fun toString(): String {
        val res = ""
        entry.map{ res + " -${it.key}:${it.value} " }
        return res
    }

    open fun removeData(condition: String) {
        H2Database().delete(sourceName, condition)
    }

    companion object {
        fun saveMultipleEntries(sourceName: String, entries: List<MutableMap<String, String?>>) {
            // This method insert or update (if exists) a record to its table
            H2Database().multipleInsertOrUpdateIfExist(sourceName, entries)
        }
    }
}