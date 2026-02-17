/*
 * FlipperKeyDictParser.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.flipper

/**
 * Parses Flipper Zero MIFARE Classic user key dictionary files.
 *
 * Format: plain text, one 12-character hex key per line.
 * Lines starting with '#' are comments. Blank lines are ignored.
 * Each key is 6 bytes (12 hex characters).
 */
object FlipperKeyDictParser {
    private val HEX_KEY_REGEX = Regex("^[0-9A-Fa-f]{12}$")

    fun parse(data: String): List<ByteArray> =
        data
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith('#') }
            .filter { HEX_KEY_REGEX.matches(it) }
            .map { hexToBytes(it) }
            .toList()

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
}
