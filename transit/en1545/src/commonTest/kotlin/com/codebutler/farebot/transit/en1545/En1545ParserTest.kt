/*
 * En1545ParserTest.kt
 *
 * Copyright 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.en1545

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalStdlibApi::class)
class En1545ParserTest {
    @Test
    fun testGetBitsFromBuffer() {
        // 0xAB = 10101011
        val data = byteArrayOf(0xAB.toByte())
        assertEquals(1, data.getBitsFromBuffer(0, 1)) // bit 0 = 1
        assertEquals(0, data.getBitsFromBuffer(1, 1)) // bit 1 = 0
        assertEquals(1, data.getBitsFromBuffer(2, 1)) // bit 2 = 1
        assertEquals(0xAB, data.getBitsFromBuffer(0, 8)) // all 8 bits
        assertEquals(5, data.getBitsFromBuffer(0, 3)) // 101 = 5
        assertEquals(3, data.getBitsFromBuffer(5, 3)) // 0xAB = 10101011, bits 5-7 = 011 = 3
    }

    @Test
    fun testGetBitsFromBufferMultiBytes() {
        // 0xFF 0x00
        val data = byteArrayOf(0xFF.toByte(), 0x00)
        assertEquals(0xFF, data.getBitsFromBuffer(0, 8))
        assertEquals(0x00, data.getBitsFromBuffer(8, 8))
        // Cross-byte read: bits 4-11 = 11110000 = 0xF0
        assertEquals(0xF0, data.getBitsFromBuffer(4, 8))
    }

    @Test
    fun testGetBitsFromBufferLeBits() {
        // 0xAB = 10101011, in LE bit order: bit0=LSB=1, bit1=1, bit2=0, bit3=1, ...
        val data = byteArrayOf(0xAB.toByte())
        assertEquals(1, data.getBitsFromBufferLeBits(0, 1)) // LSB = 1
        assertEquals(1, data.getBitsFromBufferLeBits(1, 1)) // bit 1 = 1
        assertEquals(0, data.getBitsFromBufferLeBits(2, 1)) // bit 2 = 0
        assertEquals(0xAB, data.getBitsFromBufferLeBits(0, 8)) // all bits
    }

    @Test
    fun testFixedIntegerParsing() {
        val field =
            En1545Container(
                En1545FixedInteger("FieldA", 8),
                En1545FixedInteger("FieldB", 4),
                En1545FixedInteger("FieldC", 4),
            )
        // 0xAB 0xCD -> FieldA=0xAB, FieldB=0xC, FieldC=0xD
        val data = "abcd".hexToByteArray()
        val parsed = En1545Parser.parse(data, field)
        assertEquals(0xAB, parsed.getInt("FieldA"))
        assertEquals(0xC, parsed.getInt("FieldB"))
        assertEquals(0xD, parsed.getInt("FieldC"))
    }

    @Test
    fun testFixedStringParsing() {
        // 5-bit encoded: A=1, B=2, C=3
        // "ABC" = 00001 00010 00011 = bits: 00001000100001100000... (pad to byte boundary)
        // Pack into bytes: 00001000 10000110 0000...
        val field = En1545FixedString("Name", 15)
        val data = byteArrayOf(0x08, 0x86.toByte(), 0x00)
        val parsed = En1545Parser.parse(data, field)
        assertEquals("ABC", parsed.getString("Name"))
    }

    @Test
    fun testFixedHexParsing() {
        val field = En1545FixedHex("HexData", 16)
        val data = "abcd".hexToByteArray()
        val parsed = En1545Parser.parse(data, field)
        assertEquals("abcd", parsed.getString("HexData"))
    }

    @Test
    fun testContainerParsing() {
        val field =
            En1545Container(
                En1545FixedInteger("First", 8),
                En1545Container(
                    En1545FixedInteger("InnerA", 4),
                    En1545FixedInteger("InnerB", 4),
                ),
                En1545FixedInteger("Last", 8),
            )
        val data = "abcdef".hexToByteArray()
        val parsed = En1545Parser.parse(data, field)
        assertEquals(0xAB, parsed.getInt("First"))
        assertEquals(0xC, parsed.getInt("InnerA"))
        assertEquals(0xD, parsed.getInt("InnerB"))
        assertEquals(0xEF, parsed.getInt("Last"))
    }

    @Test
    fun testBitmapParsing() {
        // Bitmap with 3 fields: bit 0, bit 1, bit 2
        // Bitmask = 101 (bits 0 and 2 present, bit 1 absent)
        val field =
            En1545Bitmap(
                En1545FixedInteger("A", 8),
                En1545FixedInteger("B", 8),
                En1545FixedInteger("C", 8),
            )
        // First 3 bits = 101 = bitmask, then A value (8 bits), then C value (8 bits)
        // 101 AAAAAAAA CCCCCCCC
        // 10111111111 00000000 0 (pad)
        // = 0xBF 0x80 0x?? but let's build it properly
        // bits: 1 0 1 | 11111111 | 00001111
        // byte0: 10111111 = 0xBF
        // byte1: 11000011 = 0xC3 (wait, let me recalculate)
        // bits: [1][0][1] [11111111] [00001111] ...
        // byte0 = 1_0_1_11111 = 10111111 = 0xBF
        // byte1 = 11_00001111 -> wait, A is 8 bits starting at bit 3
        // A: bits 3-10 = 11111111 -> byte0 bits 3-7 = 11111, byte1 bits 0-2 = 111
        // C: bits 11-18

        // Let me just use a simpler example
        // Bitmask = 111 (all 3 present): bit pattern is 111 + A(8) + B(8) + C(8) = 27 bits
        val data2 =
            byteArrayOf(
                0xFF.toByte(), // 11111111
                0xFF.toByte(), // 11111111
                0xFF.toByte(), // 11111111
                0xF0.toByte(), // 1111
            )
        val field2 =
            En1545Bitmap(
                En1545FixedInteger("A", 4),
                En1545FixedInteger("B", 4),
                En1545FixedInteger("C", 4),
            )
        // bits: 111 1111 1111 1111 ...
        // bitmask = 111 (all 3 present)
        // A = bits 3-6 = 1111 = 15
        // B = bits 7-10 = 1111 = 15
        // C = bits 11-14 = 1111 = 15
        val parsed2 = En1545Parser.parse(data2, field2)
        assertEquals(15, parsed2.getInt("A"))
        assertEquals(15, parsed2.getInt("B"))
        assertEquals(15, parsed2.getInt("C"))
    }

    @Test
    fun testBitmapPartialPresence() {
        // Bitmask for 2 fields, non-reversed: curbit starts at 1 (LSB).
        // bitmask = 01 (binary) means field A (curbit=1) is present, B (curbit=2) is absent.
        val field =
            En1545Bitmap(
                En1545FixedInteger("A", 8),
                En1545FixedInteger("B", 8),
            )
        // bits: 01 11111111 (bitmask=01, A=0xFF)
        // byte0: 01111111 = 0x7F, byte1: 11xxxxxx = 0xC0
        val data = byteArrayOf(0x7F, 0xC0.toByte())
        val parsed = En1545Parser.parse(data, field)
        assertEquals(0xFF, parsed.getInt("A"))
        assertNull(parsed.getInt("B"))
    }

    @Test
    fun testRepeatParsing() {
        // Counter length = 4 bits, field = 8-bit integer
        val field = En1545Repeat(4, En1545FixedInteger("Item", 8))
        // count=2, item0=0xAA, item1=0xBB
        // bits: 0010 10101010 10111011
        // byte0: 00101010 = 0x2A, byte1: 10101011 = 0xAB, byte2: 1011xxxx = 0xB0
        val data = byteArrayOf(0x2A, 0xAB.toByte(), 0xB0.toByte())
        val parsed = En1545Parser.parse(data, field)
        assertEquals(0xAA, parsed.getInt("Item", 0))
        assertEquals(0xBB, parsed.getInt("Item", 1))
    }

    @Test
    fun testParseDateDaysSinceEpoch() {
        val tz = TimeZone.UTC
        // Day 1 from 1997-01-01 = 1997-01-02
        val result = En1545FixedInteger.parseDate(1, tz)
        assertNotNull(result)
        val expected = LocalDate(1997, 1, 1).atStartOfDayIn(tz) + 1.days
        assertEquals(expected, result)
    }

    @Test
    fun testParseDateZeroReturnsNull() {
        assertNull(En1545FixedInteger.parseDate(0, TimeZone.UTC))
    }

    @Test
    fun testParseTimeMinutesSinceMidnight() {
        val tz = TimeZone.UTC
        // Day 100, minute 120 (2:00 AM)
        val result = En1545FixedInteger.parseTime(100, 120, tz)
        assertNotNull(result)
        val expected = LocalDate(1997, 1, 1).atStartOfDayIn(TimeZone.UTC) + 100.days + 120.minutes
        assertEquals(expected, result)
    }

    @Test
    fun testParseTimeSecondsSinceEpoch() {
        val tz = TimeZone.UTC
        val result = En1545FixedInteger.parseTimeSec(86400, tz)
        assertNotNull(result)
        // 86400 seconds = 1 day after epoch
        val expected = LocalDate(1997, 1, 1).atStartOfDayIn(TimeZone.UTC) + 1.days
        assertEquals(expected, result)
    }

    @Test
    fun testParsedGetTimeStampWithDateAndTime() {
        val field =
            En1545Container(
                En1545FixedInteger.date("Event"),
                En1545FixedInteger.time("Event"),
            )
        // Date = 100 (14 bits), Time = 120 (11 bits)
        // 100 = 0b00000001100100, 120 = 0b00001111000
        // Pack as big-endian bits: 14 bits for date + 11 bits for time = 25 bits total
        // date: 00000001100100 (100 in 14 bits)
        // time: 00001111000 (120 in 11 bits)
        // combined bits: 00000001100100 00001111000 (25 bits)
        // byte0 = bits 0-7:  00000001 = 0x01
        // byte1 = bits 8-15: 10010000 = 0x90
        // byte2 = bits 16-23: 00111100 = 0x3C
        // byte3 = bits 24:   00000000 = 0x00
        val data = byteArrayOf(0x01, 0x90.toByte(), 0x3C, 0x00)
        val parsed = En1545Parser.parse(data, field)
        val ts = parsed.getTimeStamp("Event", TimeZone.UTC)
        assertNotNull(ts)
        val expected = LocalDate(1997, 1, 1).atStartOfDayIn(TimeZone.UTC) + 100.days + 120.minutes
        assertEquals(expected, ts)
    }

    @Test
    fun testLeBitsParsing() {
        val field = En1545FixedInteger("Value", 8)
        // In LE bits: byte 0xAB = 10101011, reading LSB first gives 0xAB
        val data = byteArrayOf(0xAB.toByte())
        val parsed = En1545Parser.parseLeBits(data, field)
        assertEquals(0xAB, parsed.getInt("Value"))
    }

    @Test
    fun testParsedPlusOperator() {
        val a = En1545Parsed()
        a.insertInt("X", "", 1)
        val b = En1545Parsed()
        b.insertInt("Y", "", 2)
        val c = a + b
        assertEquals(1, c.getInt("X"))
        assertEquals(2, c.getInt("Y"))
    }

    @Test
    fun testParsedContains() {
        val p = En1545Parsed()
        p.insertInt("Exists", "", 42)
        assertEquals(true, p.contains("Exists"))
        assertEquals(false, p.contains("Missing"))
    }

    @Test
    fun testParsedInsertAndGetString() {
        val p = En1545Parsed()
        p.insertString("Name", "", "Hello")
        assertEquals("Hello", p.getString("Name"))
        assertNull(p.getString("Missing"))
    }

    @Test
    fun testParsedPathHandling() {
        val p = En1545Parsed()
        p.insertInt("Value", "root/sub", 99)
        assertEquals(99, p.getInt("Value", "root/sub"))
        assertNull(p.getInt("Value", ""))
        assertNull(p.getInt("Value"))
    }
}
