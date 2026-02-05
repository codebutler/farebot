/*
 * ErgBalanceRecord.kt
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

/**
 * Represents a balance record.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#balance-records
 */
data class ErgBalanceRecord(
    val balance: Int,
    val version: Int,
    val agencyId: Int
) : ErgRecord, Comparable<ErgBalanceRecord> {

    override fun compareTo(other: ErgBalanceRecord): Int =
        // Reverse order so highest version is first
        other.version.compareTo(this.version)

    companion object {
        fun recordFromBytes(block: ByteArray): ErgRecord? {
            return if (block[7].toInt() != 0x00 || block[8].toInt() != 0x00) {
                // Another record type gets mixed in here with non-zero values at these bytes
                null
            } else {
                ErgBalanceRecord(
                    balance = ErgRecord.byteArrayToInt(block, 11, 4),
                    version = ErgRecord.byteArrayToInt(block, 1, 2),
                    agencyId = ErgRecord.byteArrayToInt(block, 5, 2)
                )
            }
        }
    }
}
