/*
 * NextfareTopupRecord.kt
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

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * Top-up record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
data class NextfareTopupRecord(
    val timestamp: Instant,
    val credit: Int,
    val station: Int,
    val checksum: Int,
    val isAutomatic: Boolean,
) : NextfareRecord {
    companion object {
        fun recordFromBytes(
            input: ByteArray,
            timeZone: TimeZone,
        ): NextfareTopupRecord? {
            // Check if all the other data is null
            if (NextfareRecord.byteArrayToLong(input, 2, 6) == 0L) {
                return null
            }

            return NextfareTopupRecord(
                timestamp = NextfareRecord.unpackDate(input, 2, timeZone),
                credit = NextfareRecord.byteArrayToIntReversed(input, 6, 2) and 0x7FFF,
                station = NextfareRecord.byteArrayToIntReversed(input, 12, 2),
                checksum = NextfareRecord.byteArrayToIntReversed(input, 14, 2),
                isAutomatic = input[0].toInt() == 0x31,
            )
        }
    }
}
