/*
 * NextfareTransactionRecord.kt
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

package com.codebutler.farebot.transit.nextfare.record

import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/**
 * Tap record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
data class NextfareTransactionRecord(
    val type: Type,
    val timestamp: Instant,
    val mode: Int,
    val journey: Int,
    val station: Int,
    val value: Int,
    val checksum: Int,
    val isContinuation: Boolean
) : NextfareRecord, Comparable<NextfareTransactionRecord> {

    override fun compareTo(other: NextfareTransactionRecord): Int {
        // Group by journey, then by timestamp.
        return if (other.journey == this.journey) {
            this.timestamp.compareTo(other.timestamp)
        } else {
            this.journey.compareTo(other.journey)
        }
    }

    enum class Type {
        UNKNOWN,
        IGNORE,
        TRAVEL_PASS_TRIP,
        TRAVEL_PASS_SALE,
        STORED_VALUE_TRIP,
        STORED_VALUE_SALE;

        val isSale get() = (this == TRAVEL_PASS_SALE || this == STORED_VALUE_SALE)
    }

    companion object {
        private val TRIP_TYPES = mapOf(
            // SEQ, LAX: 0x05 for "Travel Pass" trips.
            0x05 to Type.TRAVEL_PASS_TRIP,
            // SEQ, LAX: 0x31 for "Stored Value" trips / transfers
            0x31 to Type.STORED_VALUE_TRIP,
            // SEQ, LAX: 0x41 for "Travel Pass" sale.
            0x41 to Type.TRAVEL_PASS_SALE,
            // LAX: 0x71 for "Stored Value" sale -- effectively recorded twice (ignored)
            0x71 to Type.IGNORE,
            // SEQ, LAX: 0x79 for "Stored Value" sale (ignored)
            0x79 to Type.IGNORE,
            // Minneapolis: 0x89 unknown transaction type, no date, only a small number around 100
            0x89 to Type.IGNORE
        )

        fun recordFromBytes(input: ByteArray, timeZone: TimeZone): NextfareTransactionRecord? {
            val transhead = input[0].toInt() and 0xFF
            val transType = TRIP_TYPES[transhead] ?: Type.UNKNOWN

            if (transType == Type.IGNORE) {
                return null
            }

            // Check if all the other data is null
            if (NextfareRecord.byteArrayToLong(input, 1, 8) == 0L) {
                return null
            }

            val mode = NextfareRecord.byteArrayToInt(input, 1, 1)
            val timestamp = NextfareRecord.unpackDate(input, 2, timeZone)
            val journey = NextfareRecord.byteArrayToIntReversed(input, 5, 2) shr 5
            val continuation = NextfareRecord.byteArrayToIntReversed(input, 5, 2) and 0x10 > 1

            var value = NextfareRecord.byteArrayToIntReversed(input, 7, 2)
            if (value > 0x8000) {
                value = -(value and 0x7FFF)
            }

            val station = NextfareRecord.byteArrayToIntReversed(input, 12, 2)
            val checksum = NextfareRecord.byteArrayToIntReversed(input, 14, 2)

            return NextfareTransactionRecord(
                type = transType,
                timestamp = timestamp,
                mode = mode,
                journey = journey,
                station = station,
                value = value,
                checksum = checksum,
                isContinuation = continuation
            )
        }
    }
}
