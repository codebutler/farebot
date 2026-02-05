/*
 * ISO7816TLVTest.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.card.iso7816

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Ported from Metrodroid's BERTLVTest.kt and SimpleTLVTest.kt
 */
@OptIn(ExperimentalStdlibApi::class)
class ISO7816TLVTest {

    // --- BER-TLV tests (from Metrodroid BERTLVTest.kt) ---

    @Test
    fun testFindDefiniteShort() {
        // tag 50 (parent, definite short)
        // -> tag 51: "hello world"
        val d = "500e510b68656c6c6f20776f726c64".hexToByteArray()
        val e = "hello world".encodeToByteArray()

        assertEquals(e.toList(), ISO7816TLV.findBERTLV(d, "51")?.toList())
    }

    @Test
    fun testFindIndefinite() {
        // tag 50 (parent, indefinite)
        // -> tag 51: "hello world"
        // end-of-contents octets
        val d = "5080510b68656c6c6f20776f726c640000".hexToByteArray()
        val e = "hello world".encodeToByteArray()

        assertEquals(e.toList(), ISO7816TLV.findBERTLV(d, "51")?.toList())
    }

    @Test
    fun testFindDefinite1() {
        // tag 50 (parent, definite long, 1 byte)
        // -> tag 51: "hello world"
        val d = "50810e510b68656c6c6f20776f726c64".hexToByteArray()
        val e = "hello world".encodeToByteArray()

        assertEquals(e.toList(), ISO7816TLV.findBERTLV(d, "51")?.toList())
    }

    @Test
    fun testFindDefinite7() {
        // tag 50 (parent, definite long, 7 bytes)
        // -> tag 51: "hello world"
        val d = "50870000000000000e510b68656c6c6f20776f726c64".hexToByteArray()
        val e = "hello world".encodeToByteArray()

        assertEquals(e.toList(), ISO7816TLV.findBERTLV(d, "51")?.toList())
    }

    @Test
    fun testFindDefinite126() {
        // tag 50 (parent, definite long, 126 bytes)
        // -> tag 51: "hello world"
        val d = "50fe".hexToByteArray() +
            ByteArray(125) +
            "0e510b68656c6c6f20776f726c64".hexToByteArray()
        val e = "hello world".encodeToByteArray()

        assertEquals(e.toList(), ISO7816TLV.findBERTLV(d, "51")?.toList())
    }

    @Test
    fun testFindDefiniteReallyLong() {
        // tag 50 (parent, definite long, 0xffffffffffffffff bytes)
        // -> tag 51: "hello world"
        val d = "5088".hexToByteArray() +
            ByteArray(8) { 0xff.toByte() } +
            "0e510b68656c6c6f20776f726c64".hexToByteArray()

        // Should fail
        assertNull(ISO7816TLV.findBERTLV(d, "51"))
    }

    @Test
    fun testZeroLengthAtEnd() {
        // tag 50 (parent, definite short)
        // -> tag 51: "hello world"
        // -> tag 52: (zero bytes at end of value)
        val d = "5010510b68656c6c6f20776f726c645201".hexToByteArray()
        val e = "hello world".encodeToByteArray()

        assertEquals(e.toList(), ISO7816TLV.findBERTLV(d, "51")?.toList())
        assertEquals(emptyList(), ISO7816TLV.findBERTLV(d, "52")?.toList())
    }

    // --- Simple-TLV tests (from Metrodroid SimpleTLVTest.kt) ---

    @Test
    fun testPCSCAtrSimpleTLV() {
        // Historical bytes from PC/SC-compatible reader on FeliCa. PC/SC specification treats the
        // historical bytes in the ATR as a Simple-TLV object, rather than a Compact-TLV object.
        val i = "4f0ca00000030611003b00000000".hexToByteArray()
        val expected = listOf(Pair(0x4f, "a00000030611003b00000000".hexToByteArray()))

        val result = ISO7816TLV.simpleTlvIterate(i).toList()
        assertEquals(expected.size, result.size)
        assertEquals(expected[0].first, result[0].first)
        assertEquals(expected[0].second.toList(), result[0].second.toList())
    }

    @Test
    fun testSimpleTlvWithNulls() {
        val i = "0100020100ff00fe03112233".hexToByteArray()
        val expected = listOf(
            // Empty tag: 01
            Pair(0x02, "00".hexToByteArray()),
            // Empty tag: FF
            Pair(0xfe, "112233".hexToByteArray())
        )

        val result = ISO7816TLV.simpleTlvIterate(i).toList()
        assertEquals(expected.size, result.size)
        for (idx in expected.indices) {
            assertEquals(expected[idx].first, result[idx].first)
            assertEquals(expected[idx].second.toList(), result[idx].second.toList())
        }
    }

    @Test
    fun testSimpleTlvLongLength() {
        val i = "0fff00031122330a000b0211220cff00000d0122".hexToByteArray()
        val expected = listOf(
            // Long length = 3 bytes
            Pair(0x0f, "112233".hexToByteArray()),
            // Empty tag: 0A
            Pair(0x0b, "1122".hexToByteArray()),
            // Empty long tag: 0C
            Pair(0x0d, "22".hexToByteArray())
        )

        val result = ISO7816TLV.simpleTlvIterate(i).toList()
        assertEquals(expected.size, result.size)
        for (idx in expected.indices) {
            assertEquals(expected[idx].first, result[idx].first)
            assertEquals(expected[idx].second.toList(), result[idx].second.toList())
        }
    }
}
