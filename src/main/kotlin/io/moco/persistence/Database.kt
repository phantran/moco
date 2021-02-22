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