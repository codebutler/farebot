/*
 * PN533RawClassicTest.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.keymanager.crypto1

import com.codebutler.farebot.keymanager.pn533.PN533RawClassic
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Tests for [PN533RawClassic] static helper functions.
 *
 * These are pure unit tests that do not require real PN533 hardware.
 */
class PN533RawClassicTest {
    @Test
    fun testBuildAuthCommand() {
        // AUTH command for key A (0x60), block 0
        val cmd = PN533RawClassic.buildAuthCommand(0x60, 0)
        assertEquals(4, cmd.size, "Auth command should be 4 bytes: [keyType, block, CRC_L, CRC_H]")
        assertEquals(0x60.toByte(), cmd[0], "First byte should be key type")
        assertEquals(0x00.toByte(), cmd[1], "Second byte should be block index")

        // Verify CRC is correct ISO 14443-3A CRC of [0x60, 0x00]
        val expectedCrc = Crypto1Auth.crcA(byteArrayOf(0x60, 0x00))
        assertEquals(expectedCrc[0], cmd[2], "CRC low byte mismatch")
        assertEquals(expectedCrc[1], cmd[3], "CRC high byte mismatch")

        // AUTH command for key B (0x61), block 4
        val cmdB = PN533RawClassic.buildAuthCommand(0x61, 4)
        assertEquals(0x61.toByte(), cmdB[0])
        assertEquals(0x04.toByte(), cmdB[1])
        val expectedCrcB = Crypto1Auth.crcA(byteArrayOf(0x61, 0x04))
        assertEquals(expectedCrcB[0], cmdB[2])
        assertEquals(expectedCrcB[1], cmdB[3])
    }

    @Test
    fun testBuildReadCommand() {
        // READ command for block 0
        val cmd = PN533RawClassic.buildReadCommand(0)
        assertEquals(4, cmd.size, "Read command should be 4 bytes: [0x30, block, CRC_L, CRC_H]")
        assertEquals(0x30.toByte(), cmd[0], "First byte should be MIFARE READ (0x30)")
        assertEquals(0x00.toByte(), cmd[1], "Second byte should be block index")

        // Verify CRC
        val expectedCrc = Crypto1Auth.crcA(byteArrayOf(0x30, 0x00))
        assertEquals(expectedCrc[0], cmd[2], "CRC low byte mismatch")
        assertEquals(expectedCrc[1], cmd[3], "CRC high byte mismatch")

        // READ command for block 63
        val cmd63 = PN533RawClassic.buildReadCommand(63)
        assertEquals(0x30.toByte(), cmd63[0])
        assertEquals(63.toByte(), cmd63[1])
        val expectedCrc63 = Crypto1Auth.crcA(byteArrayOf(0x30, 63))
        assertEquals(expectedCrc63[0], cmd63[2])
        assertEquals(expectedCrc63[1], cmd63[3])
    }

    @Test
    fun testParseNonce() {
        // 4 bytes big-endian: 0xDEADBEEF
        val bytes = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val nonce = PN533RawClassic.parseNonce(bytes)
        assertEquals(0xDEADBEEFu, nonce)

        // Zero nonce
        val zeroBytes = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        assertEquals(0u, PN533RawClassic.parseNonce(zeroBytes))

        // Max value
        val maxBytes = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        assertEquals(0xFFFFFFFFu, PN533RawClassic.parseNonce(maxBytes))

        // Verify byte order: MSB first
        val ordered = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        assertEquals(0x01020304u, PN533RawClassic.parseNonce(ordered))
    }

    @Test
    fun testBytesToUInt() {
        // Same as parseNonce but using the explicit bytesToUInt method
        val bytes = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        assertEquals(0x12345678u, PN533RawClassic.bytesToUInt(bytes))

        val zero = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        assertEquals(0u, PN533RawClassic.bytesToUInt(zero))

        val max = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        assertEquals(0xFFFFFFFFu, PN533RawClassic.bytesToUInt(max))

        // Single high byte
        val highByte = byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x00)
        assertEquals(0x80000000u, PN533RawClassic.bytesToUInt(highByte))
    }

    @Test
    fun testUintToBytes() {
        val bytes = PN533RawClassic.uintToBytes(0x12345678u)
        assertContentEquals(byteArrayOf(0x12, 0x34, 0x56, 0x78), bytes)

        val zero = PN533RawClassic.uintToBytes(0u)
        assertContentEquals(byteArrayOf(0x00, 0x00, 0x00, 0x00), zero)

        val max = PN533RawClassic.uintToBytes(0xFFFFFFFFu)
        assertContentEquals(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()), max)

        val deadbeef = PN533RawClassic.uintToBytes(0xDEADBEEFu)
        assertContentEquals(
            byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()),
            deadbeef,
        )
    }

    @Test
    fun testUintToBytesRoundtrip() {
        // Convert UInt -> bytes -> UInt should be identity
        val values =
            listOf(
                0u,
                1u,
                0x12345678u,
                0xDEADBEEFu,
                0xFFFFFFFFu,
                0x80000000u,
                0x00000001u,
                0xCAFEBABEu,
            )
        for (value in values) {
            val bytes = PN533RawClassic.uintToBytes(value)
            val result = PN533RawClassic.bytesToUInt(bytes)
            assertEquals(value, result, "Roundtrip failed for 0x${value.toString(16)}")
        }

        // Convert bytes -> UInt -> bytes should be identity
        val byteArrays =
            listOf(
                byteArrayOf(0x01, 0x02, 0x03, 0x04),
                byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte(), 0x01),
                byteArrayOf(0x00, 0x00, 0x00, 0x00),
                byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            )
        for (bytes in byteArrays) {
            val value = PN533RawClassic.bytesToUInt(bytes)
            val result = PN533RawClassic.uintToBytes(value)
            assertContentEquals(bytes, result, "Roundtrip failed for byte array")
        }
    }
}
