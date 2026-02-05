/*
 * ErgPreambleRecord.kt
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

package com.codebutler.farebot.transit.erg.record

import com.codebutler.farebot.transit.erg.ErgTransitInfo

/**
 * Represents a preamble record.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#preamble-record
 */
data class ErgPreambleRecord(
    val cardSerial: String?
) : ErgRecord {

    companion object {
        private val OLD_CARD_ID = byteArrayOf(0x00, 0x00, 0x00)

        fun recordFromBytes(input: ByteArray): ErgPreambleRecord {
            if (!input.copyOfRange(0, ErgTransitInfo.SIGNATURE.size)
                    .contentEquals(ErgTransitInfo.SIGNATURE)) {
                throw IllegalArgumentException("Preamble signature does not match")
            }

            val serialBytes = input.copyOfRange(10, 13)
            val cardSerial = if (serialBytes.contentEquals(OLD_CARD_ID)) {
                null
            } else {
                input.copyOfRange(10, 14).joinToString("") {
                    (it.toInt() and 0xFF).toString(16).padStart(2, '0')
                }.uppercase()
            }

            return ErgPreambleRecord(cardSerial)
        }
    }
}
