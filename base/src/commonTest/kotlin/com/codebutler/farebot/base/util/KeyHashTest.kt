/*
 * KeyHashTest.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
 * This test validates [HashUtils.checkKeyHash] such that:
 *
 * 1. The KeyHash algorithm hasn't changed.
 * 2. The arguments are working in an expected way.
 *
 * Please do not change this implementation, as this will break other card readers that depend on
 * it.
 *
 * Ported from Metrodroid's KeyHashTest.kt (portable subset, excluding ClassicSectorKey tests).
 */
@OptIn(ExperimentalStdlibApi::class)
class KeyHashTest {
    @Test
    fun testIncorrectKeyHash() {
        // Empty list
        assertEquals(-1, HashUtils.checkKeyHash(MAD_KEY, SALT0))

        // Test with just 1 possible answer
        assertEquals(-1, HashUtils.checkKeyHash(MAD_KEY, SALT0, MAD_HASH1))

        // Then test with multiple
        assertEquals(
            -1,
            HashUtils.checkKeyHash(
                MAD_KEY,
                SALT0,
                MAD_HASH1,
                MAD_HASH2,
                DEFAULT_HASH0,
                DEFAULT_HASH1,
                DEFAULT_HASH2,
            ),
        )
        assertEquals(
            -1,
            HashUtils.checkKeyHash(
                MAD_KEY,
                SALT1,
                MAD_HASH0,
                MAD_HASH2,
                DEFAULT_HASH0,
                DEFAULT_HASH1,
                DEFAULT_HASH2,
            ),
        )
        assertEquals(
            -1,
            HashUtils.checkKeyHash(
                MAD_KEY,
                SALT2,
                MAD_HASH0,
                MAD_HASH1,
                DEFAULT_HASH0,
                DEFAULT_HASH1,
                DEFAULT_HASH2,
            ),
        )

        assertEquals(
            -1,
            HashUtils.checkKeyHash(
                DEFAULT_KEY,
                SALT0,
                MAD_HASH0,
                MAD_HASH1,
                MAD_HASH2,
                DEFAULT_HASH1,
                DEFAULT_HASH2,
            ),
        )
        assertEquals(
            -1,
            HashUtils.checkKeyHash(
                DEFAULT_KEY,
                SALT1,
                MAD_HASH0,
                MAD_HASH1,
                MAD_HASH2,
                DEFAULT_HASH0,
                DEFAULT_HASH2,
            ),
        )
        assertEquals(
            -1,
            HashUtils.checkKeyHash(
                DEFAULT_KEY,
                SALT2,
                MAD_HASH0,
                MAD_HASH1,
                MAD_HASH2,
                DEFAULT_HASH0,
                DEFAULT_HASH1,
            ),
        )
    }

    @Test
    fun testCorrectKeyHash() {
        // Checking when there is one right answer.
        assertEquals(0, HashUtils.checkKeyHash(MAD_KEY, SALT0, MAD_HASH0))
        assertEquals(0, HashUtils.checkKeyHash(MAD_KEY, SALT1, MAD_HASH1))
        assertEquals(0, HashUtils.checkKeyHash(MAD_KEY, SALT2, MAD_HASH2))

        assertEquals(0, HashUtils.checkKeyHash(DEFAULT_KEY, SALT0, DEFAULT_HASH0))
        assertEquals(0, HashUtils.checkKeyHash(DEFAULT_KEY, SALT1, DEFAULT_HASH1))
        assertEquals(0, HashUtils.checkKeyHash(DEFAULT_KEY, SALT2, DEFAULT_HASH2))
    }

    @Test
    fun testOffsetCorrectKeyHash() {
        assertEquals(
            1,
            HashUtils.checkKeyHash(
                MAD_KEY,
                SALT1,
                MAD_HASH0,
                MAD_HASH1,
            ),
        )
        assertEquals(
            1,
            HashUtils.checkKeyHash(
                MAD_KEY,
                SALT1,
                MAD_HASH0,
                MAD_HASH1,
                MAD_HASH2,
            ),
        )

        assertEquals(
            2,
            HashUtils.checkKeyHash(
                MAD_KEY,
                SALT2,
                MAD_HASH0,
                MAD_HASH1,
                MAD_HASH2,
            ),
        )
    }

    @Test
    fun testRepeatedCorrectKeyHash() {
        assertEquals(
            0,
            HashUtils.checkKeyHash(
                DEFAULT_KEY,
                SALT0,
                DEFAULT_HASH0,
                DEFAULT_HASH0,
                DEFAULT_HASH1,
            ),
        )
        assertEquals(
            0,
            HashUtils.checkKeyHash(
                DEFAULT_KEY,
                SALT0,
                DEFAULT_HASH0,
                DEFAULT_HASH1,
                DEFAULT_HASH0,
            ),
        )

        assertEquals(
            2,
            HashUtils.checkKeyHash(
                DEFAULT_KEY,
                SALT1,
                DEFAULT_HASH0,
                DEFAULT_HASH0,
                DEFAULT_HASH1,
            ),
        )
        assertEquals(
            2,
            HashUtils.checkKeyHash(
                DEFAULT_KEY,
                SALT1,
                DEFAULT_HASH0,
                DEFAULT_HASH0,
                DEFAULT_HASH1,
                DEFAULT_HASH1,
            ),
        )
    }

    private fun checkVector(
        input: String,
        output: String,
    ) {
        val digest = md5(input.hexToByteArray()).toHexString()
        assertEquals(output, digest, "Hash of <$input> failed")
    }

    @Test
    fun testMd5vectors() {
        checkVector("", "d41d8cd98f00b204e9800998ecf8427e")
        checkVector("6162636465666768696a6b6c6d6e6f707172737475767778797a", "c3fcd3d76192e4007dfb496cca67e13b")
    }

    companion object {
        private val MAD_KEY = "A0A1A2A3A4A5".hexToByteArray()
        private val DEFAULT_KEY = "FFFFFFFFFFFF".hexToByteArray()

        private const val SALT0 = "sodium chloride"
        private const val MAD_HASH0 = "fc18681fd880307349238c72268aae3b"
        private const val DEFAULT_HASH0 = "1a0aea4daffab36129fc4a760567a823"

        private const val SALT1 = "bath"
        private const val MAD_HASH1 = "93bf0db4fc97682b9d79dc667c046b88"
        private const val DEFAULT_HASH1 = "878156605169c3070573998b35e08846"

        private const val SALT2 = "cracked pepper"
        private const val MAD_HASH2 = "42451d2b7c8338b7d4f60313c5f4e3f3"
        private const val DEFAULT_HASH2 = "dfc7fcfdcff15daf0b71226cbf87cb32"
    }
}
