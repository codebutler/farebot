/*
 * HashUtils.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
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

package com.codebutler.farebot.base.util

object HashUtils {

    // --- CRC tables ---

    private val CRC16_IBM_TABLE by lazy { getCRCTableReversed(0xa001) }
    private val CRC8_NXP_TABLE by lazy { getCRCTableDirect(0x1d, 8) }
    const val CRC8_NXP_INITIAL = 0xc7

    private fun getCRCTableReversed(poly: Int) =
        (0..255).map { v ->
            (0..7).fold(v) { cur, _ ->
                if ((cur and 1) != 0)
                    (cur shr 1) xor poly
                else
                    (cur shr 1)
            }
        }.toIntArray()

    private fun getCRCTableDirect(poly: Int, bits: Int): IntArray {
        val extPoly = poly or (1 shl bits)
        val mask = 1 shl (bits - 1)
        return (0..255).map { v ->
            (0..7).fold(v) { cur, _ ->
                if ((cur and mask) != 0)
                    (cur shl 1) xor extPoly
                else
                    (cur shl 1)
            }
        }.toIntArray()
    }

    // --- CRC calculation ---

    private fun calculateCRCReversed(data: ByteArray, init: Int, table: IntArray) =
        data.fold(init) { cur1, b -> (cur1 shr 8) xor table[(cur1 xor b.toInt()) and 0xff] }

    private fun calculateCRC8(data: ByteArray, init: Int, table: IntArray) =
        data.fold(init) { cur1, b -> table[(cur1 xor b.toInt()) and 0xff] }

    fun calculateCRC16IBM(data: ByteArray, crc: Int = 0) =
        calculateCRCReversed(data, crc, CRC16_IBM_TABLE)

    fun calculateCRC8NXP(data: ByteArray, crc: Int = CRC8_NXP_INITIAL): Int =
        calculateCRC8(data, crc, CRC8_NXP_TABLE)

    fun calculateCRC8NXP(vararg data: ByteArray): Int =
        data.fold(CRC8_NXP_INITIAL) { crc, block -> calculateCRC8NXP(block, crc) }

    // --- Key hash checking ---

    /**
     * Checks if a salted MD5 hash of a key matches any of the expected hashes.
     *
     * Hash format: lowercase(hex(md5(salt + key + salt)))
     *
     * @param key The key bytes to check
     * @param salt Salt string prepended and appended to key before hashing
     * @param expectedHashes Expected hash values to match against
     * @return Index of matching hash in expectedHashes, or -1 if no match
     */
    fun checkKeyHash(key: ByteArray, salt: String, vararg expectedHashes: String): Int {
        if (expectedHashes.isEmpty()) return -1

        val saltBytes = salt.encodeToByteArray()
        val toHash = saltBytes + key + saltBytes
        val digest = md5(toHash).hex()

        return expectedHashes.indexOf(digest)
    }

    /**
     * Checks if keyA or keyB of a sector matches any of the expected hashes.
     *
     * @param keyA Key A bytes (nullable)
     * @param keyB Key B bytes (nullable)
     * @param salt Salt string
     * @param expectedHashes Expected hash values to match against
     * @return Index of matching hash, or -1 if no match
     */
    fun checkKeyHash(keyA: ByteArray?, keyB: ByteArray?, salt: String, vararg expectedHashes: String): Int {
        if (keyA != null) {
            val a = checkKeyHash(keyA, salt, *expectedHashes)
            if (a != -1) return a
        }
        if (keyB != null) {
            return checkKeyHash(keyB, salt, *expectedHashes)
        }
        return -1
    }
}
