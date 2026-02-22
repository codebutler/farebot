/*
 * Varint.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.flipper

object Varint {
    fun encode(value: Int): ByteArray {
        val result = mutableListOf<Byte>()
        var v = value
        while (v > 0x7F) {
            result.add(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
        result.add((v and 0x7F).toByte())
        return result.toByteArray()
    }

    /** Returns (decoded value, number of bytes consumed). */
    fun decode(
        data: ByteArray,
        offset: Int,
    ): Pair<Int, Int> {
        var result = 0
        var shift = 0
        var pos = offset
        while (pos < data.size) {
            val b = data[pos].toInt() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            pos++
            if (b and 0x80 == 0) break
            shift += 7
        }
        return result to (pos - offset)
    }
}
