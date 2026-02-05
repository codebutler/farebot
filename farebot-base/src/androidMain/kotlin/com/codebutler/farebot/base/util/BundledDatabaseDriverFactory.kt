package com.codebutler.farebot.base.util

import android.content.Context
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import java.io.File

actual class BundledDatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(dbName: String, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
        val dbFile = File(context.cacheDir, dbName)
        if (!dbFile.exists()) {
            context.assets.open(dbName).use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
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
        return AndroidSqliteDriver(noOpSchema, context, dbName)
    }
}
