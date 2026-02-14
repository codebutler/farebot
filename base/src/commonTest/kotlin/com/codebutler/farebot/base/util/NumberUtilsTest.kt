/*
 * NumberUtilsTest.kt
 *
 * Ported from Metrodroid's NumberTest.kt
 * (https://github.com/metrodroid/metrodroid)
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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ported from Metrodroid's NumberTest.kt (portable subset).
 */
class NumberUtilsTest {
    @Test
    fun testBCD() {
        assertTrue(NumberUtils.isValidBCD(0x123456))
        assertFalse(NumberUtils.isValidBCD(0x1234a6))
        assertEquals(0x123456, NumberUtils.intToBCD(123456))
    }

    @Test
    fun testDigitSum() {
        assertEquals(60, NumberUtils.getDigitSum(12345678912345))
    }

    @Test
    fun testLog10() {
        assertEquals(0, NumberUtils.log10floor(9))
        assertEquals(1, NumberUtils.log10floor(10))
        assertEquals(1, NumberUtils.log10floor(99))
        assertEquals(2, NumberUtils.log10floor(100))
        assertEquals(6, NumberUtils.log10floor(1234567))
    }

    @Test
    fun testDigits() {
        assertContentEquals(
            intArrayOf(1, 2, 3, 4, 5, 6, 7),
            NumberUtils.digitsOf(1234567),
        )
    }
}
