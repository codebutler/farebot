/*
 * En1545Repeat.kt
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
 * EN1545 Repeated Fields
 *
 * A repeated field consists of a counter (fixed integer) containing the number of repetitions,
 * followed by the field values.
 */
class En1545Repeat(
    private val ctrLen: Int,
    private val field: En1545Field,
) : En1545Field {
    @Suppress("NAME_SHADOWING")
    override fun parseField(
        b: ByteArray,
        off: Int,
        path: String,
        holder: En1545Parsed,
        bitParser: En1545Bits,
    ): Int {
        var off = off
        if (off + ctrLen > b.size * 8) return off + ctrLen
        val ctr = bitParser(b, off, ctrLen)

        off += ctrLen
        for (i in 0 until ctr) {
            off = field.parseField(b, off, "$path/$i", holder, bitParser)
        }
        return off
    }
}
