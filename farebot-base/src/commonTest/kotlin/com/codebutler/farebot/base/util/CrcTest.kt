/*
 * CrcTest.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.base.util

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from Metrodroid's CrcTest.kt.
 */
@OptIn(ExperimentalStdlibApi::class)
class CrcTest {
    @Test
    fun testIBM() {
        assertEquals(expected = 0x0000, actual = HashUtils.calculateCRC16IBM(byteArrayOf()))
        assertEquals(expected = 0xc0c1, actual = HashUtils.calculateCRC16IBM(byteArrayOf(1)))
        assertEquals(expected = 0x4321, actual = HashUtils.calculateCRC16IBM("IBM".encodeToByteArray()))
        assertEquals(expected = 0xe52c, actual = HashUtils.calculateCRC16IBM("Metrodroid".encodeToByteArray()))
        assertEquals(expected = 0x2699, actual = HashUtils.calculateCRC16IBM("CrcTest".encodeToByteArray()))
    }

    @Test
    fun testNXP() {
        assertEquals(expected = 0xc7, actual = HashUtils.calculateCRC8NXP(byteArrayOf()))
        assertEquals(expected = 0x66, actual = HashUtils.calculateCRC8NXP(byteArrayOf(0)))
        assertEquals(expected = 0x7b, actual = HashUtils.calculateCRC8NXP(byteArrayOf(1)))
        assertEquals(
            expected = 0xb0,
            actual = HashUtils.calculateCRC8NXP(
                "0003e103e103e103e103e103e1000000000000000000000000000000000000".hexToByteArray()
            )
        )
    }
}
