@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.codebutler.farebot.base.util

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual class BundledDatabaseDriverFactory {
    actual fun createDriver(
        dbName: String,
        schema: SqlSchema<QueryResult.Value<Unit>>,
    ): SqlDriver {
        val bundlePath =
            NSBundle.mainBundle.pathForResource(
                dbName.removeSuffix(".db3").removeSuffix(".db"),
                ofType = if (dbName.endsWith(".db3")) "db3" else "db",
            )

        if (bundlePath != null) {
            // Copy bundled DB to sqliter's default path: Library/Application Support/databases/
            val appSupportDirs =
                NSSearchPathForDirectoriesInDomains(
                    NSApplicationSupportDirectory,
                    NSUserDomainMask,
                    true,
                )
            val appSupportDir =
                appSupportDirs.firstOrNull() as? String
                    ?: error("No Application Support directory found")
            val dbDir = "$appSupportDir/databases"

            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(dbDir)) {
                fileManager.createDirectoryAtPath(
                    dbDir,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null,
                )
            }

            val destPath = "$dbDir/$dbName"
            if (fileManager.fileExistsAtPath(destPath)) {
                fileManager.removeItemAtPath(destPath, error = null)
            }
            fileManager.copyItemAtPath(bundlePath, toPath = destPath, error = null)

            val noOpSchema =
                object : SqlSchema<QueryResult.Value<Unit>> {
                    override val version: Long = 3

                    override fun create(driver: SqlDriver): QueryResult.Value<Unit> = QueryResult.Unit

                    override fun migrate(
                        driver: SqlDriver,
                        oldVersion: Long,
                        newVersion: Long,
                        vararg callbacks: AfterVersion,
                    ): QueryResult.Value<Unit> = QueryResult.Unit
                }

            return NativeSqliteDriver(noOpSchema, dbName)
        } else {
            return NativeSqliteDriver(schema, dbName)
        }
    }
}
