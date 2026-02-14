/*
 * ClassicAccessBits.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2018 Google
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic

/**
 * Parses MIFARE Classic access control bits from the sector trailer.
 *
 * The access bits are stored in bytes 6-8 of the sector trailer block (block 3).
 * They determine read/write permissions for each block in the sector per key type.
 *
 * Each block has a 3-bit access condition (C1, C2, C3) that maps to specific
 * read/write/increment/decrement permissions for Key A and Key B.
 */
class ClassicAccessBits private constructor(
    private val c1: Int,
    private val c2: Int,
    private val c3: Int,
) {
    /**
     * Parse access bits from the 3-byte access bits field (bytes 6-8 of trailer).
     */
    constructor(raw: ByteArray) : this(
        c1 = (raw[1].toInt() and 0xf0) shr 4,
        c2 = raw[2].toInt() and 0xf,
        c3 = (raw[2].toInt() and 0xf0) shr 4,
    )

    /**
     * Get the 3-bit access condition value for a given slot (block 0-3).
     */
    fun getSlot(slot: Int): Int =
        (((c1 shr slot) and 0x1) shl 2) or
            (((c2 shr slot) and 0x1) shl 1) or
            ((c3 shr slot) and 0x1)

    /**
     * Whether a data block at the given slot is readable with the specified key type.
     * @param slot Block index (0-2 for data blocks)
     * @param useKeyB true if authenticating with Key B, false for Key A
     */
    fun isDataBlockReadable(
        slot: Int,
        useKeyB: Boolean,
    ): Boolean =
        when (getSlot(slot)) {
            0, 1, 2, 4, 6 -> true
            3, 5 -> useKeyB && !isKeyBReadable
            7 -> false
            else -> false
        }

    enum class AccessLevel {
        NEVER,
        KEY_A,
        KEY_B,
        KEY_AB,
    }

    data class BlockAccess(
        val read: AccessLevel,
        val write: AccessLevel,
        val increment: AccessLevel,
        val decrement: AccessLevel,
    )

    /**
     * Get the parsed access permissions for a data block slot.
     * @param slot Block index (0-2 for data blocks, 3 for trailer)
     */
    fun getBlockAccess(slot: Int): BlockAccess? {
        val ab = if (isKeyBReadable) AccessLevel.KEY_A else AccessLevel.KEY_AB
        val b = if (isKeyBReadable) AccessLevel.NEVER else AccessLevel.KEY_B
        return when (getSlot(slot)) {
            0 -> BlockAccess(ab, ab, ab, ab)
            1 -> BlockAccess(ab, AccessLevel.NEVER, AccessLevel.NEVER, ab)
            2 -> BlockAccess(ab, AccessLevel.NEVER, AccessLevel.NEVER, AccessLevel.NEVER)
            3 -> BlockAccess(b, b, AccessLevel.NEVER, AccessLevel.NEVER)
            4 -> BlockAccess(ab, b, AccessLevel.NEVER, AccessLevel.NEVER)
            5 -> BlockAccess(b, AccessLevel.NEVER, AccessLevel.NEVER, AccessLevel.NEVER)
            6 -> BlockAccess(ab, b, b, ab)
            7 -> BlockAccess(AccessLevel.NEVER, AccessLevel.NEVER, AccessLevel.NEVER, AccessLevel.NEVER)
            else -> null
        }
    }

    /**
     * Whether Key B can be read from the sector trailer using Key A.
     * When true, Key B cannot be used for authentication (it's stored as data).
     */
    val isKeyBReadable: Boolean
        get() = getSlot(3) in listOf(0, 1, 2)

    companion object {
        /**
         * Validate the access bits checksum.
         * The inverted bits in byte 6 and lower nibble of byte 7 must match
         * the non-inverted bits in upper nibble of byte 7 and byte 8.
         */
        fun isValid(accBits: ByteArray): Boolean {
            val c123inv = (accBits[0].toInt() and 0xff) or ((accBits[1].toInt() and 0xf) shl 8)
            val c123 = ((accBits[1].toInt() and 0xf0) shr 4) or ((accBits[2].toInt() and 0xff) shl 4)
            return c123inv == c123.inv() and 0xfff
        }
    }
}
