/*
 * En1545FixedString.kt
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

class En1545FixedString(private val name: String, private val len: Int) : En1545Field {

    override fun parseField(b: ByteArray, off: Int, path: String, holder: En1545Parsed, bitParser: En1545Bits): Int {
        val string = parseString(b, off, len, bitParser)
        if (string != null)
            holder.insertString(name, path, string)
        return off + len
    }

    private fun parseString(bin: ByteArray, start: Int, length: Int, bitParser: En1545Bits): String? {
        var i = start
        var j = 0
        var lastNonSpace = 0
        val ret = StringBuilder()
        while (i + 4 < start + length && i + 4 < bin.size * 8) {
            val bl: Int
            try {
                bl = bitParser(bin, i, 5)
            } catch (_: Exception) {
                return null
            }

            if (bl == 0 || bl == 31) {
                if (j != 0) {
                    ret.append(' ')
                    j++
                }
            } else {
                ret.append(('A'.code + bl - 1).toChar())
                lastNonSpace = j
                j++
            }
            i += 5
        }
        return try {
            ret.substring(0, lastNonSpace + 1)
        } catch (_: Exception) {
            ret.toString()
        }
    }
}
