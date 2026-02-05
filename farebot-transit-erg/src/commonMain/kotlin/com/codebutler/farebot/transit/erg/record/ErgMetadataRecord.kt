/*
 * ErgMetadataRecord.kt
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
 * Represents a metadata record.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#metadata-record
 */
data class ErgMetadataRecord(
    val cardSerial: ByteArray,
    val epochDate: Int,
    val agencyId: Int
) : ErgRecord {

    companion object {
        fun recordFromBytes(input: ByteArray): ErgMetadataRecord {
            val agencyId = ErgRecord.byteArrayToInt(input, 2, 2)
            val epochDays = ErgRecord.byteArrayToInt(input, 5, 2)
            val cardSerial = input.copyOfRange(7, 11)
            return ErgMetadataRecord(cardSerial, epochDays, agencyId)
        }
    }
}
