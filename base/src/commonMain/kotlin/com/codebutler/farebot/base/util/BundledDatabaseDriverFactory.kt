package com.codebutler.farebot.base.util

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

expect class BundledDatabaseDriverFactory {
    fun createDriver(
        dbName: String,
        schema: SqlSchema<QueryResult.Value<Unit>>,
    ): SqlDriver
}
