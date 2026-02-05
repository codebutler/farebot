/*
 * ByteArrayBits.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.en1545

/**
 * Extract bits from a byte array in big-endian bit order.
 * Bit 0 = MSB of byte 0, bit 7 = LSB of byte 0, bit 8 = MSB of byte 1, etc.
 */
fun ByteArray.getBitsFromBuffer(startBit: Int, length: Int): Int {
    if (length <= 0 || length > 32) throw IllegalArgumentException("Invalid bit length: $length")
    var result = 0
    for (i in startBit until startBit + length) {
        val byteIndex = i / 8
        val bitIndex = 7 - (i % 8)
        if (byteIndex >= size) throw IndexOutOfBoundsException("Bit $i out of bounds for ${size}-byte array")
        result = (result shl 1) or ((this[byteIndex].toInt() shr bitIndex) and 1)
    }
    return result
}

/**
 * Extract bits from a byte array in little-endian bit order.
 * Bit 0 = LSB of byte 0, bit 7 = MSB of byte 0, bit 8 = LSB of byte 1, etc.
 */
fun ByteArray.getBitsFromBufferLeBits(startBit: Int, length: Int): Int {
    if (length <= 0 || length > 32) throw IllegalArgumentException("Invalid bit length: $length")
    var result = 0
    for (i in 0 until length) {
        val bitPos = startBit + i
        val byteIndex = bitPos / 8
        val bitIndex = bitPos % 8
        if (byteIndex >= size) throw IndexOutOfBoundsException("Bit $bitPos out of bounds for ${size}-byte array")
        result = result or (((this[byteIndex].toInt() shr bitIndex) and 1) shl i)
    }
    return result
}
