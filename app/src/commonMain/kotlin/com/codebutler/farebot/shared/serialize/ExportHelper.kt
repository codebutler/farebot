/*
 * ExportHelper.kt
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

import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.RawCard
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Helper functions for export operations.
 * Matches Metrodroid's ExportHelper patterns for compatibility.
 */
object ExportHelper {
    /**
     * Generates a filename for a card dump export.
     *
     * @param tagId The card's UID as a byte array
     * @param scannedAt Timestamp when the card was scanned
     * @param format The export format
     * @param generation Used for handling duplicate filenames in a ZIP (0 for first file)
     * @return A filename in the format "FareBot-{tagId}-{datetime}.{extension}"
     */
    fun makeFilename(
        tagId: ByteArray,
        scannedAt: Instant,
        format: ExportFormat,
        generation: Int = 0,
    ): String {
        val tagIdHex = tagId.hex()
        val dt = formatDateTimeForFilename(scannedAt)
        val genSuffix = if (generation != 0) "-$generation" else ""
        return "FareBot-$tagIdHex-$dt$genSuffix.${format.extension}"
    }

    /**
     * Generates a filename for a card dump export.
     *
     * @param card The card dump to generate a filename for
     * @param format The export format (defaults to JSON)
     * @param generation Used for handling duplicate filenames in a ZIP (0 for first file)
     * @return A filename in the format "FareBot-{tagId}-{datetime}.{extension}"
     */
    fun makeFilename(
        card: RawCard<*>,
        format: ExportFormat = ExportFormat.JSON,
        generation: Int = 0,
    ): String = makeFilename(card.tagId(), card.scannedAt(), format, generation)

    /**
     * Generates a filename for bulk export of multiple cards.
     *
     * @param format The export format
     * @param timestamp Export timestamp (defaults to current time)
     * @return A filename in the format "FareBot-export-{datetime}.{extension}"
     */
    fun makeBulkExportFilename(
        format: ExportFormat = ExportFormat.JSON,
        timestamp: Instant =
            kotlin.time.Clock.System
                .now(),
    ): String {
        val dt = formatDateTimeForFilename(timestamp)
        return "farebot-export-$dt.${format.extension}"
    }

    /**
     * Generates a filename for a ZIP archive containing multiple card dumps.
     *
     * @param timestamp Export timestamp (defaults to current time)
     * @return A filename in the format "FareBot-export-{datetime}.zip"
     */
    fun makeZipFilename(
        timestamp: Instant =
            kotlin.time.Clock.System
                .now(),
    ): String {
        val dt = formatDateTimeForFilename(timestamp)
        return "farebot-export-$dt.zip"
    }

    /**
     * Formats a timestamp for use in filenames.
     * Format: YYYYMMDD-HHmmss (no colons or spaces for filesystem compatibility)
     */
    private fun formatDateTimeForFilename(instant: Instant): String {
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return buildString {
            append(local.year.toString().padStart(4, '0'))
            append((local.month.ordinal + 1).toString().padStart(2, '0'))
            append(local.day.toString().padStart(2, '0'))
            append("-")
            append(local.hour.toString().padStart(2, '0'))
            append(local.minute.toString().padStart(2, '0'))
            append(local.second.toString().padStart(2, '0'))
        }
    }

    /**
     * Gets the file extension from a filename.
     */
    fun getExtension(filename: String): String? {
        val dotIndex = filename.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < filename.length - 1) {
            filename.substring(dotIndex + 1).lowercase()
        } else {
            null
        }
    }

    /**
     * Determines the export format from a filename.
     */
    fun getFormatFromFilename(filename: String): ExportFormat? {
        val ext = getExtension(filename) ?: return null
        return ExportFormat.fromExtension(ext)
    }
}
