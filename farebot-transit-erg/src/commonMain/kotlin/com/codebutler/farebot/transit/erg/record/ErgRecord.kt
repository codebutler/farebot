/*
 * ErgRecord.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.erg.record

/**
 * Represents a record inside of an ERG MIFARE Classic based card.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC
 */
interface ErgRecord {
    companion object {
        fun byteArrayToInt(
            data: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            var result = 0
            for (i in 0 until length) {
                result = (result shl 8) or (data[offset + i].toInt() and 0xFF)
            }
            return result
        }

        fun getBitsFromBuffer(
            data: ByteArray,
            startBit: Int,
            length: Int,
        ): Int {
            var result = 0
            for (i in startBit until (startBit + length)) {
                val byteIndex = i / 8
                val bitIndex = 7 - (i % 8)
                if (byteIndex < data.size) {
                    result = (result shl 1) or ((data[byteIndex].toInt() shr bitIndex) and 1)
                }
            }
            return result
        }
    }
}
