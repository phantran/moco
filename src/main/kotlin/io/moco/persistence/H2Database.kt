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
import org.h2.jdbcx.JdbcConnectionPool
import java.sql.Connection

class H2Database : Database() {

    private val logger = MoCoLogger()

    override fun createTable(tableName: String, schema: String) {
        try {
            val statement = "CREATE TABLE IF NOT EXISTS $tableName ( $schema );"
            connectionsPool.connection.use { con ->
                con?.createStatement().use { st -> st?.execute(statement) }
            }
        } catch (ex: Exception) {
            println(ex.printStackTrace().toString())
            logger.error("Cannot create database table $tableName")
        }
    }

    override fun dropTable(tableName: String) {
        try {
            connectionsPool.connection.use { con ->
                val statement = "DROP TABLE IF EXISTS $tableName;"
                con?.createStatement().use { st -> st?.execute(statement) }
            }
        } catch (ex: Exception) {
            logger.error("Cannot delete database table $tableName")
        }
    }

    fun dropAllMoCoTables() {
        for (table in MoCoTables.keys) {
            try {
                connectionsPool.connection.use { con ->
                    val statement = "DROP TABLE IF EXISTS $table;"
                    con?.createStatement().use { st -> st?.execute(statement) }
                }
            } catch (ex: Exception) {
                logger.error("Cannot delete database table $table")
            }
        }
    }

    override fun dropDatabase(dbName: String) {
        try {
            connectionsPool.connection.use { con ->
                val statement = "DROP DATABASE dbName;"
                con?.createStatement().use { st -> st?.execute(statement) }
            }
        } catch (ex: Exception) {
            logger.error("Cannot drop database $dbName")
        }
    }

    override fun insert(table: String, data: Map<String, String>) {
        try {
            connectionsPool.connection.use { con ->
                val columns = "(" + data.keys.joinToString(separator = ",") + ")"
                val values = "('" + data.values.joinToString(separator = "','") + "')"
                val statement = "INSERT INTO $table $columns VALUES $values; "
                con?.createStatement().use { st -> st?.execute(statement) }
            }
        } catch (ex: Exception) {
            logger.error("Error while inserting data $data to $table")
        }

    }

    override fun insertOrUpdateIfExist(table: String, data: Map<String, String>) {
        try {
            connectionsPool.connection.use { con ->
                val statement = getInsertUpdateOnDuplicateStm(table, data)
                con?.createStatement().use { st -> st?.execute(statement) }
            }
        } catch (ex: Exception) {
            println(ex.printStackTrace())
            logger.error("Error while inserting data $data to $table")
        }
    }

    private fun getInsertUpdateOnDuplicateStm(table: String, data: Map<String, String?>): String {
        val columns = "(" + data.keys.joinToString(separator = ",") + ")"
        val values = "('" + data.values.joinToString(separator = "','") + "')"
        val onExist = data.map { "${it.key}=\'${it.value}\' " }.joinToString(separator = ",")
        return "INSERT INTO $table $columns VALUES $values ON DUPLICATE KEY UPDATE $onExist;"
    }

    override fun multipleInsertOrUpdateIfExist(table: String, data: List<Map<String, String?>>) {
        val temp = data.map { getInsertUpdateOnDuplicateStm(table, it) }
        val stm = temp.joinToString(" ")
        try {
            connectionsPool.connection.use { con ->
                con?.createStatement().use { st -> st?.execute(stm) }
            }
        } catch (ex: Exception) {
            println(ex.printStackTrace())
            logger.error("Error while inserting data $data to $table")
        }
    }


    override fun delete(table: String, condition: String) {
        try {
            connectionsPool.connection.use { con ->
                val statement = "DELETE FROM $table WHERE $condition;"
                con?.createStatement().use { st -> st?.execute(statement) }
            }
        } catch (ex: Exception) {
            logger.error("Error while deleting data from $table with condition $condition")
        }
    }

    override fun deleteAll(table: String) {
        try {
            val statement = "DELETE FROM $table;"
            connectionsPool.connection.use { con ->
                con.createStatement().use { st -> st?.execute(statement) }
            }
        } catch (ex: Exception) {
            logger.error("Error while deleting all rows of $table")
        }
    }

    override fun fetchOne(connection: Connection, table: Any, condition: String, select: List<String>): ResultSet? {
        val query = "SELECT ${select.joinToString(separator = ",")} FROM $table WHERE $condition LIMIT 1;"
        return try {
            val stm = connection.createStatement()
            return stm?.executeQuery(query)
        } catch (ex: Exception) {
            logger.error(ex.printStackTrace().toString())
            logger.error("Error while fetch one from $table with condition $condition $query")
            null
        }
    }

    override fun fetch(connection: Connection, table: Any, condition: String, select: List<String>): ResultSet? {
        val where = if (condition.isNotEmpty()) "WHERE $condition" else ""
        val query = "SELECT ${select.joinToString(separator = ",")} FROM $table " + where + ";"
        return try {
            val stm = connection.createStatement()
            stm?.executeQuery(query)
        } catch (ex: Exception) {
            println(ex.printStackTrace())
            logger.error("Error while fetch all from $table with condition $condition $query")
            null
        }
    }

    fun executeQuery(connection: Connection, query: String): ResultSet? {
        return try {
            val stm = connection.createStatement()
            stm?.executeQuery(query)
        } catch (ex: Exception) {
            println(ex.printStackTrace())
            logger.error("Error while execute query $query")
            null
        }
    }

    companion object {
        private lateinit var connectionsPool: JdbcConnectionPool
        fun initPool(url: String, user: String, password: String ) {
            connectionsPool = JdbcConnectionPool.create(url, user, password)
        }

        fun getConnection(): Connection {
            return connectionsPool.connection
        }

        val MoCoTables = mapOf(
            "ProjectMeta" to ProjectMeta.schema,
            "ProgressClassTest" to ProgressClassTest.schema,
            "PersistentMutationResult" to PersistentMutationResult.schema,
            "ProjectTestHistory" to ProjectTestHistory.schema,
            "MutantsBlackList" to MutantsBlackList.schema,
            "TestsCutMapping" to TestsCutMapping.schema

        )

        fun shutDownDB() {
            if (::connectionsPool.isInitialized) {
                connectionsPool.connection.use { con ->
                    con?.createStatement().use { st ->
                        st?.execute("SHUTDOWN")
                    }
                }
            }
        }

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

    fun initDBTablesIfNotExists() {
        MoCoTables.map { createTable(it.key, it.value) }
    }
}