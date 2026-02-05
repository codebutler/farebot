/*
 * ErgPurseRecord.kt
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
 * Represents a "purse" type record.
 *
 * These are simple transactions where there is either a credit or debit from the purse value.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#purse-records
 */
data class ErgPurseRecord(
    val agency: Int,
    val day: Int,
    val minute: Int,
    val isCredit: Boolean,
    val transactionValue: Int,
    val isTrip: Boolean
) : ErgRecord {

    companion object {
        fun recordFromBytes(block: ByteArray): ErgRecord? {
            val isCredit: Boolean
            val isTrip: Boolean
            when (block[3].toInt()) {
                0x09, 0x0D -> {
                    // Manly: 0x09, CHC: 0x0D — purse debit
                    isCredit = false
                    isTrip = false
                }
                0x08 -> {
                    // CHC, Manly — purse credit
                    isCredit = true
                    isTrip = false
                }
                0x02 -> {
                    // CHC: For every non-paid trip, CHC puts in a 0x02
                    // For every paid trip, CHC puts a 0x0d (purse debit) and 0x02
                    isCredit = false
                    isTrip = true
                }
                else -> return null
            }

            val record = ErgPurseRecord(
                agency = ErgRecord.byteArrayToInt(block, 1, 2),
                day = ErgRecord.getBitsFromBuffer(block, 32, 20),
                minute = ErgRecord.getBitsFromBuffer(block, 52, 12),
                transactionValue = ErgRecord.byteArrayToInt(block, 8, 4),
                isCredit = isCredit,
                isTrip = isTrip
            )

            require(record.day >= 0) { "Day < 0" }
            require(record.minute in 0..1440) { "Minute out of range: ${record.minute}" }

            return record
        }
    }
}
