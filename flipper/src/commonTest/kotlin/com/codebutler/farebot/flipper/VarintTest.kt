/*
 * VarintTest.kt
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

class VarintTest {
    @Test
    fun testEncodeSmallValue() {
        assertContentEquals(byteArrayOf(0x01), Varint.encode(1))
        assertContentEquals(byteArrayOf(0x7F), Varint.encode(127))
    }

    @Test
    fun testEncodeTwoByteValue() {
        // 128 = 0x80 -> varint [0x80, 0x01]
        assertContentEquals(byteArrayOf(0x80.toByte(), 0x01), Varint.encode(128))
        // 300 = 0x12C -> varint [0xAC, 0x02]
        assertContentEquals(byteArrayOf(0xAC.toByte(), 0x02), Varint.encode(300))
    }

    @Test
    fun testEncodeZero() {
        assertContentEquals(byteArrayOf(0x00), Varint.encode(0))
    }

    @Test
    fun testDecodeSmallValue() {
        val (value, bytesRead) = Varint.decode(byteArrayOf(0x01), 0)
        assertEquals(1, value)
        assertEquals(1, bytesRead)
    }

    @Test
    fun testDecodeTwoByteValue() {
        val (value, bytesRead) = Varint.decode(byteArrayOf(0xAC.toByte(), 0x02), 0)
        assertEquals(300, value)
        assertEquals(2, bytesRead)
    }

    @Test
    fun testRoundTrip() {
        for (v in listOf(0, 1, 127, 128, 255, 256, 16383, 16384, 65535, 1_000_000)) {
            val encoded = Varint.encode(v)
            val (decoded, _) = Varint.decode(encoded, 0)
            assertEquals(v, decoded, "Round-trip failed for $v")
        }
    }
}
