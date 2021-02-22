package io.moco.persistence

import java.lang.Exception
import java.sql.ResultSet


open class H2Database(
    url: String = "",
    user: String = "",
    password: String = ""
) : Database(url, user, password) {

    override fun createTable(tableName: String, schema: Map<String, String>) {
        try {
            val schemaText = StringBuilder("")
            for (key in schema.keys) {
                schemaText.append(key + " " + schema[key] + ", ")
            }

            val statement = "CREATE TABLE IF NOT EXISTS $tableName ( $schemaText );"
            connection?.createStatement().use { st -> st?.execute(statement) }
        } catch (ex: Exception) {
            println("Cannot create database table $tableName")
        }
    }

    override fun dropTable(tableName: String) {
        try {
            val statement = "DROP TABLE IF EXISTS $tableName;"
            connection?.createStatement().use { st -> st?.execute(statement) }
        } catch (ex: Exception) {
            println("Cannot delete database table $tableName")
        }
    }

    override fun dropDatabase(dbName: String) {
        try {
            val statement = "DROP DATABASE dbName;"
            connection?.createStatement().use { st -> st?.execute(statement) }
        } catch (ex: Exception) {
            println("Cannot drop database $dbName")
        }
    }

    override fun insert(table: String, data: Map<String, String>) {
        try {
            val columns = "(" + data.keys.joinToString(separator = ",") + ")"
            val values = "('" + data.values.joinToString(separator = "','") + "')"
            val statement = "INSERT INTO $table $columns VALUES $values; "
            connection?.createStatement().use { st -> st?.execute(statement) }
        } catch (ex: Exception) {
            println("Error while inserting data $data to $table")
        }

    }

    override fun delete(table: String, condition: String) {
        try {
            val statement = "DELETE FROM $table WHERE $condition;"
            connection?.createStatement().use { st -> st?.execute(statement) }
        } catch (ex: Exception) {
            println("Error while deleting data from $table with condition $condition")
        }
    }

    override fun deleteAll(table: String) {
        try {
            val statement = "DELETE FROM $table;"
            connection?.createStatement().use { st -> st?.execute(statement) }
        } catch (ex: Exception) {
            println("Error while deleting all rows of $table")
        }
    }

    override fun fetchOne(table: Any, condition: String, select: List<String>): ResultSet? {
        val query = "SELECT ${select.joinToString(separator = ",")} FROM $table WHERE $condition LIMIT 1;"
        return try {
            val stm = connection?.createStatement()
            return stm?.executeQuery(query)
        } catch (ex: Exception) {
            println(ex.printStackTrace())
            println("Error while fetch one from $table with condition $condition $query")
            null
        }
    }

    override fun fetchAll(table: Any, condition: String, select: List<String>): ResultSet? {
        val where = if (condition.isNotEmpty()) "WHERE $condition" else ""
        val query = "SELECT ${select.joinToString(separator = ",")} FROM $table " + where + ";"
        try {
            val stm = connection?.createStatement()
            return stm?.executeQuery(query)
        } catch (ex: Exception) {
            println("Error while fetch all from $table with condition $condition $query")
            return null

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