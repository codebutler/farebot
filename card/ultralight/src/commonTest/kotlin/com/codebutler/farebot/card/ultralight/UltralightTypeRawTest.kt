/*
 * UltralightTypeRawTest.kt
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

package com.codebutler.farebot.card.ultralight

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [UltralightTypeRaw.parse] which maps GET_VERSION and AUTH_1
 * protocol responses to [UltralightCard.UltralightType] values.
 */
class UltralightTypeRawTest {
    /**
     * Creates a mock GET_VERSION response with the given product code and storage size.
     * Format: 8 bytes, product code at index 2, storage size at index 6.
     */
    private fun makeVersionResponse(
        productCode: Int,
        storageSize: Int,
    ): ByteArray =
        ByteArray(8).also {
            it[2] = productCode.toByte()
            it[6] = storageSize.toByte()
        }

    // --- EV1 detection (product code 0x03) ---

    @Test
    fun testEv1Mf0ul11() {
        val raw = UltralightTypeRaw(versionCmd = makeVersionResponse(0x03, 0x0b))
        assertEquals(UltralightCard.UltralightType.EV1_MF0UL11, raw.parse())
    }

    @Test
    fun testEv1Mf0ul21() {
        val raw = UltralightTypeRaw(versionCmd = makeVersionResponse(0x03, 0x0e))
        assertEquals(UltralightCard.UltralightType.EV1_MF0UL21, raw.parse())
    }

    @Test
    fun testEv1UnknownStorageSize() {
        val raw = UltralightTypeRaw(versionCmd = makeVersionResponse(0x03, 0xFF))
        assertEquals(UltralightCard.UltralightType.UNKNOWN, raw.parse())
    }

    // --- NTAG detection (product code 0x04) ---

    @Test
    fun testNtag213() {
        val raw = UltralightTypeRaw(versionCmd = makeVersionResponse(0x04, 0x0F))
        assertEquals(UltralightCard.UltralightType.NTAG213, raw.parse())
    }

    @Test
    fun testNtag215() {
        val raw = UltralightTypeRaw(versionCmd = makeVersionResponse(0x04, 0x11))
        assertEquals(UltralightCard.UltralightType.NTAG215, raw.parse())
    }

    @Test
    fun testNtag216() {
        val raw = UltralightTypeRaw(versionCmd = makeVersionResponse(0x04, 0x13))
        assertEquals(UltralightCard.UltralightType.NTAG216, raw.parse())
    }

    @Test
    fun testNtagUnknownStorageSize() {
        val raw = UltralightTypeRaw(versionCmd = makeVersionResponse(0x04, 0xFF))
        assertEquals(UltralightCard.UltralightType.UNKNOWN, raw.parse())
    }

    // --- Unknown product codes ---

    @Test
    fun testUnknownProductCode() {
        val raw = UltralightTypeRaw(versionCmd = makeVersionResponse(0x05, 0x0b))
        assertEquals(UltralightCard.UltralightType.UNKNOWN, raw.parse())
    }

    // --- Invalid version response sizes ---

    @Test
    fun testVersionResponseTooShort() {
        val raw = UltralightTypeRaw(versionCmd = ByteArray(4))
        assertEquals(UltralightCard.UltralightType.UNKNOWN, raw.parse())
    }

    @Test
    fun testVersionResponseTooLong() {
        val raw = UltralightTypeRaw(versionCmd = ByteArray(12))
        assertEquals(UltralightCard.UltralightType.UNKNOWN, raw.parse())
    }

    // --- AUTH_1 fallback detection ---

    @Test
    fun testAuth1RepliesTrue_UltralightC() {
        val raw = UltralightTypeRaw(repliesToAuth1 = true)
        assertEquals(UltralightCard.UltralightType.MF0ICU2, raw.parse())
    }

    @Test
    fun testAuth1RepliesFalse_Ultralight() {
        val raw = UltralightTypeRaw(repliesToAuth1 = false)
        assertEquals(UltralightCard.UltralightType.MF0ICU1, raw.parse())
    }

    @Test
    fun testAuth1Null_Ultralight() {
        val raw = UltralightTypeRaw(repliesToAuth1 = null)
        assertEquals(UltralightCard.UltralightType.MF0ICU1, raw.parse())
    }

    @Test
    fun testDefaultConstructor_Ultralight() {
        val raw = UltralightTypeRaw()
        assertEquals(UltralightCard.UltralightType.MF0ICU1, raw.parse())
    }

    // --- Version command takes precedence over auth1 ---

    @Test
    fun testVersionCmdTakesPrecedenceOverAuth1() {
        val raw =
            UltralightTypeRaw(
                versionCmd = makeVersionResponse(0x04, 0x0F),
                repliesToAuth1 = true,
            )
        assertEquals(UltralightCard.UltralightType.NTAG213, raw.parse())
    }
}
