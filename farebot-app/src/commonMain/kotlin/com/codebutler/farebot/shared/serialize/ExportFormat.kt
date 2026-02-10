/*
 * ExportFormat.kt
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

/**
 * Supported export formats for card data.
 */
enum class ExportFormat(val extension: String, val mimeType: String) {
    /**
     * JSON format - matches Metrodroid's current format for interoperability.
     */
    JSON("json", "application/json"),

    /**
     * XML format - legacy format for compatibility with older FareBot/Metrodroid exports.
     */
    XML("xml", "application/xml");

    companion object {
        fun fromExtension(ext: String): ExportFormat? = entries.find {
            it.extension.equals(ext, ignoreCase = true)
        }

        fun fromMimeType(mime: String): ExportFormat? = entries.find {
            it.mimeType.equals(mime, ignoreCase = true)
        }
    }
}
