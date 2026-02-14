/*
 * ExportMetadata.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2024 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.shared.serialize

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Metadata included in exported card data files.
 * Provides information about the source application and export timestamp.
 */
@Serializable
data class ExportMetadata(
    /**
     * Application name that created the export.
     */
    val appName: String = APP_NAME,
    /**
     * Application version code (numeric).
     */
    val versionCode: Int = 1,
    /**
     * Application version name (human-readable).
     */
    val versionName: String = "1.0.0",
    /**
     * ISO 8601 timestamp of when the export was created.
     */
    val exportedAt: Instant = Clock.System.now(),
    /**
     * Export format version for forward/backward compatibility.
     */
    val formatVersion: Int = FORMAT_VERSION,
) {
    companion object {
        const val APP_NAME = "FareBot"
        const val FORMAT_VERSION = 1

        fun create(
            versionCode: Int,
            versionName: String,
        ): ExportMetadata =
            ExportMetadata(
                appName = APP_NAME,
                versionCode = versionCode,
                versionName = versionName,
                exportedAt = Clock.System.now(),
                formatVersion = FORMAT_VERSION,
            )
    }
}
