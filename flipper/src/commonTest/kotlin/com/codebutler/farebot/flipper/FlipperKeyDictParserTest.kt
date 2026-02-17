/*
 * FlipperKeyDictParserTest.kt
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

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FlipperKeyDictParserTest {

    @Test
    fun testParseValidDictionary() {
        val input = """
            # Flipper NFC user dictionary
            FFFFFFFFFFFF
            A0A1A2A3A4A5
            D3F7D3F7D3F7

            000000000000
        """.trimIndent()

        val keys = FlipperKeyDictParser.parse(input)
        assertEquals(4, keys.size)
        assertContentEquals(
            byteArrayOf(
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            ),
            keys[0],
        )
        assertContentEquals(
            byteArrayOf(
                0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(),
                0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte(),
            ),
            keys[1],
        )
    }

    @Test
    fun testSkipsCommentsAndBlanks() {
        val input = """
            # Comment

            # Another comment
            FFFFFFFFFFFF

        """.trimIndent()

        val keys = FlipperKeyDictParser.parse(input)
        assertEquals(1, keys.size)
    }

    @Test
    fun testSkipsInvalidKeys() {
        val input = """
            FFFFFFFFFFFF
            TOOSHORT
            FFFFFFFFFFFF00
            A0A1A2A3A4A5
        """.trimIndent()

        val keys = FlipperKeyDictParser.parse(input)
        assertEquals(2, keys.size) // Only valid 12-char hex strings
    }

    @Test
    fun testEmptyInput() {
        val keys = FlipperKeyDictParser.parse("")
        assertEquals(0, keys.size)
    }
}
