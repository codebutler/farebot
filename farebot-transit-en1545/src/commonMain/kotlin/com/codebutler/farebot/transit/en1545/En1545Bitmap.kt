/*
 * En1545Bitmap.kt
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
 * EN1545 Bitmaps
 *
 * Consists of:
 * - 1 bit for every field present inside the bitmap.
 * - Where a bit is non-zero, the embedded field.
 */
class En1545Bitmap private constructor(
    private val infix: En1545Field?,
    private val fields: List<En1545Field>,
    private val reversed: Boolean
) : En1545Field {

    constructor(vararg fields: En1545Field, reversed: Boolean = false) : this(
        infix = null,
        fields = fields.toList(),
        reversed = reversed
    )

    @Suppress("NAME_SHADOWING")
    override fun parseField(b: ByteArray, off: Int, path: String, holder: En1545Parsed, bitParser: En1545Bits): Int {
        var off = off
        val bitmask: Int
        try {
            bitmask = bitParser(b, off, fields.size)
        } catch (_: Exception) {
            return off + fields.size
        }

        off += fields.size
        if (infix != null)
            off = infix.parseField(b, off, path, holder, bitParser)
        var curbit = if (reversed) (1 shl (fields.size - 1)) else 1
        for (el in fields) {
            if (bitmask and curbit != 0)
                off = el.parseField(b, off, path, holder, bitParser)
            curbit = if (reversed) curbit shr 1 else curbit shl 1
        }
        return off
    }

    companion object {
        fun infixBitmap(infix: En1545Container, vararg fields: En1545Field, reversed: Boolean = false): En1545Field =
            En1545Bitmap(infix, fields.toList(), reversed = reversed)
    }
}
