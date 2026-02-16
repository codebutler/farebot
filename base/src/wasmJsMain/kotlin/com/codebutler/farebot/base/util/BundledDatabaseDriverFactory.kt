package com.codebutler.farebot.base.util

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

actual class BundledDatabaseDriverFactory {
    actual fun createDriver(
        dbName: String,
        schema: SqlSchema<QueryResult.Value<Unit>>,
    ): SqlDriver =
        throw UnsupportedOperationException("SQLDelight BundledDatabaseDriverFactory is not yet available on wasmJs")
}
