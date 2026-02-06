package com.codebutler.farebot.base.util

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

actual class BundledDatabaseDriverFactory {
    actual fun createDriver(dbName: String, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
        val tmpDir = System.getProperty("java.io.tmpdir")
        val dbFile = File(tmpDir, "farebot-$dbName")
        if (!dbFile.exists()) {
            val stream = this::class.java.classLoader?.getResourceAsStream(dbName)
            if (stream != null) {
                stream.use { input ->
                    dbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        val noOpSchema = object : SqlSchema<QueryResult.Value<Unit>> {
            override val version: Long = schema.version
            override fun create(driver: SqlDriver): QueryResult.Value<Unit> = QueryResult.Unit
            override fun migrate(
                driver: SqlDriver,
                oldVersion: Long,
                newVersion: Long,
                vararg callbacks: AfterVersion
            ): QueryResult.Value<Unit> = QueryResult.Unit
        }
        val url = if (dbFile.exists()) {
            "jdbc:sqlite:${dbFile.absolutePath}"
        } else {
            JdbcSqliteDriver.IN_MEMORY
        }
        return JdbcSqliteDriver(url, properties = Properties(), schema = noOpSchema)
    }
}
