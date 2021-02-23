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

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

open class Database (
    url: String = "",
    user: String = "",
    password: String = "") {

    open val connection: Connection? = DriverManager.getConnection(url, user, password)

    open fun createTable(tableName: String, schema: Map<String, String>) {}
    open fun dropTable(tableName: String) {}
    open fun dropDatabase(dbName: String) {}

    open fun insert(table: String, data: Map<String, String>) {}
    open fun delete(table: String, condition: String = "") {}
    open fun deleteAll(table: String) {}
    open fun fetchOne(table: Any, condition: String, select: List<String> = listOf("*")): ResultSet?  {return null}
    open fun fetchAll(table: Any, condition: String = "", select: List<String> = listOf("*")): ResultSet?  {return null}

    fun closeConnection() {
        connection?.close()
    }
}