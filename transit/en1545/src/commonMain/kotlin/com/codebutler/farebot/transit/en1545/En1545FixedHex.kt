/*
 * En1545FixedHex.kt
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

class En1545FixedHex(
    private val name: String,
    private val len: Int,
) : En1545Field {
    override fun parseField(
        b: ByteArray,
        off: Int,
        path: String,
        holder: En1545Parsed,
        bitParser: En1545Bits,
    ): Int {
        if (off + len <= b.size * 8) {
            var res = ""
            var i = len
            while (i > 0) {
                if (i >= 8) {
                    var t = bitParser(b, off + i - 8, 8).toString(16)
                    if (t.length == 1) t = "0$t"
                    res = t + res
                    i -= 8
                    continue
                }
                if (i >= 4) {
                    res = bitParser(b, off + i - 4, 4).toString(16) + res
                    i -= 4
                    continue
                }
                res = bitParser(b, off, i).toString(16) + res
                break
            }
            holder.insertString(name, path, res)
        }
        return off + len
    }
}
