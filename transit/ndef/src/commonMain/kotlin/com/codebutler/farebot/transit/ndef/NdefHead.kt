/*
 * NdefHead.kt
 *
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

package com.codebutler.farebot.transit.ndef

import com.codebutler.farebot.base.util.byteArrayToInt

data class NdefHead(
    val me: Boolean,
    val cf: Boolean,
    val tnf: Int,
    val typeLen: Int,
    val payloadLen: Int,
    val idLen: Int?,
    val headLen: Int,
) {
    companion object {
        fun parse(
            data: ByteArray,
            ptrStart: Int,
        ): NdefHead? {
            var ptr = ptrStart
            val head = data[ptr]
            val mb = (head.toInt() and 0x80) != 0
            if (mb != (ptr == 0)) {
                return null
            }
            val me = (head.toInt() and 0x40) != 0
            val cf = (head.toInt() and 0x20) != 0
            val sr = (head.toInt() and 0x10) != 0
            val il = (head.toInt() and 0x08) != 0
            val tnf = (head.toInt() and 0x07)
            ptr++
            val typeLen = data[ptr++].toInt() and 0xff
            val payloadLenSize = if (sr) 1 else 4
            val payloadLen = data.byteArrayToInt(ptr, payloadLenSize)
            ptr += payloadLenSize
            val idLen = if (il) data[ptr++].toInt() and 0xff else null
            return NdefHead(
                me = me,
                cf = cf,
                tnf = tnf,
                typeLen = typeLen,
                payloadLen = payloadLen,
                idLen = idLen,
                headLen = ptr - ptrStart,
            )
        }
    }
}
