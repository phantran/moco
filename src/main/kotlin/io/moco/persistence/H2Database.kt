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
import java.lang.Exception
import java.sql.ResultSet


class H2Database(
    url: String = "",
    user: String = "",
    password: String = ""
) : Database(url, user, password) {

    private val logger = MoCoLogger()

    override fun createTable(tableName: String, schema: Map<String, String>) {
        try {
            val schemaText = StringBuilder("")
            for (key in schema.keys) {
                schemaText.append(key + " " + schema[key] + ", ")
            }

            val statement = "CREATE TABLE IF NOT EXISTS $tableName ( $schemaText );"
            connection?.createStatement().use { st -> st?.execute(statement) }
        } catch (ex: Exception) {
            logger.error("Cannot create database table $tableName")
        }
    }

    override fun dropTable(tableName: String) {
        try {
            val statement = "DROP TABLE IF EXISTS $tableName;"
            connection?.createStatement().use { st -> st?.execute(statement) }
        } catch (ex: Exception) {
            logger.error( "Cannot delete database table $tableName")
        }
    }

    override fun dropDatabase(dbName: String) {
        try {
            val statement = "DROP DATABASE dbName;"
            connection?.createStatement().use { st -> st?.execute(statement) }
        } catch (ex: Exception) {
            logger.error( "Cannot drop database $dbName")
        }
    }

    override fun insert(table: String, data: Map<String, String>) {
        try {
            val columns = "(" + data.keys.joinToString(separator = ",") + ")"
            val values = "('" + data.values.joinToString(separator = "','") + "')"
            val statement = "INSERT INTO $table $columns VALUES $values; "
            connection?.createStatement().use { st -> st?.execute(statement) }
        } catch (ex: Exception) {
            logger.error( "Error while inserting data $data to $table")
        }

    }

    override fun delete(table: String, condition: String) {
        try {
            val statement = "DELETE FROM $table WHERE $condition;"
            connection?.createStatement().use { st -> st?.execute(statement) }
        } catch (ex: Exception) {
            logger.error( "Error while deleting data from $table with condition $condition")
        }
    }

    override fun deleteAll(table: String) {
        try {
            val statement = "DELETE FROM $table;"
            connection?.createStatement().use { st -> st?.execute(statement) }
        } catch (ex: Exception) {
            logger.error( "Error while deleting all rows of $table")
        }
    }

    override fun fetchOne(table: Any, condition: String, select: List<String>): ResultSet? {
        val query = "SELECT ${select.joinToString(separator = ",")} FROM $table WHERE $condition LIMIT 1;"
        return try {
            val stm = connection?.createStatement()
            return stm?.executeQuery(query)
        } catch (ex: Exception) {
            logger.error(ex.printStackTrace().toString())
            logger.error( "Error while fetch one from $table with condition $condition $query")
            null
        }
    }

    override fun fetchAll(table: Any, condition: String, select: List<String>): ResultSet? {
        val where = if (condition.isNotEmpty()) "WHERE $condition" else ""
        val query = "SELECT ${select.joinToString(separator = ",")} FROM $table " + where + ";"
        return try {
            val stm = connection?.createStatement()
            stm?.executeQuery(query)
        } catch (ex: Exception) {
            logger.error( "Error while fetch all from $table with condition $condition $query")
            null

        }
    }

    companion object {
        fun printResults(rs: ResultSet) {
            val rsMeta = rs.metaData
            val columnsNumber = rsMeta.columnCount
            while (rs.next()) {
                for (i in 1..columnsNumber) {
                    if (i > 1) print(",  ")
                    val columnValue = rs.getString(i)
                    print(columnValue + " " + rsMeta.getColumnName(i))
                }
                println("")
            }

        }
    }
}