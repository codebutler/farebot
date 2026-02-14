/*
 * NumberUtils.kt
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

object NumberUtils {
    // --- Hex formatting ---

    fun byteToHex(v: Byte): String = "0x" + (v.toInt() and 0xff).toString(16)

    fun intToHex(v: Int): String = "0x" + v.toString(16)

    fun longToHex(v: Long): String = "0x" + v.toString(16)

    // --- BCD ---

    fun convertBCDtoInteger(data: Int): Int {
        var res = 0
        for (i in 0..7) {
            res = res * 10 + ((data shr (4 * (7 - i))) and 0xf)
        }
        return res
    }

    fun convertBCDtoInteger(data: Byte): Int {
        val d = data.toInt()
        val h = (d and 0xf0) shr 4
        val l = (d and 0x0f)
        return (if (h >= 9) 90 else h * 10) + (if (l >= 9) 9 else l)
    }

    fun intToBCD(input: Int): Int {
        var cur = input
        var off = 0
        var res = 0
        while (cur > 0) {
            val dig = cur % 10
            res = res or (dig shl off)
            off += 4
            cur /= 10
        }
        return res
    }

    fun isValidBCD(data: Int): Boolean =
        (0..7).all {
            ((data shr (4 * it)) and 0xf) in 0..9
        }

    // --- String formatting ---

    fun zeroPad(
        value: String,
        minDigits: Int,
    ): String {
        if (value.length >= minDigits) return value
        return CharArray(minDigits - value.length) { '0' }.concatToString() + value
    }

    fun zeroPad(
        value: Int,
        minDigits: Int,
    ): String = zeroPad(value.toString(), minDigits)

    fun zeroPad(
        value: Long,
        minDigits: Int,
    ): String = zeroPad(value.toString(), minDigits)

    fun groupString(
        value: String,
        separator: String,
        vararg groups: Int,
    ): String {
        val ret = StringBuilder()
        var ptr = 0
        for (g in groups) {
            ret.append(value, ptr, ptr + g).append(separator)
            ptr += g
        }
        ret.append(value, ptr, value.length)
        return ret.toString()
    }

    fun formatNumber(
        value: Long,
        separator: String,
        vararg groups: Int,
    ): String {
        val minDigit = groups.sum()
        val unformatted = zeroPad(value, minDigit)
        val numDigit = unformatted.length
        var last = numDigit - minDigit
        val ret = StringBuilder()
        ret.append(unformatted, 0, last)
        for (g in groups) {
            ret.append(unformatted, last, last + g).append(separator)
            last += g
        }
        return ret.substring(0, ret.length - 1)
    }

    // --- Digit manipulation ---

    fun getDigitSum(value: Long): Int {
        var dig = value
        var digsum = 0
        while (dig > 0) {
            digsum += (dig % 10).toInt()
            dig /= 10
        }
        return digsum
    }

    fun digitsOf(integer: Long): IntArray = integer.toString().map { it.digitToInt() }.toIntArray()

    fun getBitsFromInteger(
        buffer: Int,
        iStartBit: Int,
        iLength: Int,
    ): Int = (buffer shr iStartBit) and ((1 shl iLength) - 1)

    // --- Power / log ---

    fun pow(
        a: Int,
        b: Int,
    ): Long {
        var ret: Long = 1
        repeat(b) {
            ret *= a.toLong()
        }
        return ret
    }

    fun log10floor(value: Int): Int {
        var mul = 1
        var ctr = 0
        while (value >= 10 * mul) {
            ctr++
            mul *= 10
        }
        return ctr
    }
}

val Byte.hexString: String get() = NumberUtils.byteToHex(this)
val Int.hexString: String get() = NumberUtils.intToHex(this)
val Long.hexString: String get() = NumberUtils.longToHex(this)
