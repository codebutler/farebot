/*
 * NextfareTravelPassRecord.kt
 *
 * Copyright 2016-2019 Michael Farrell <micolous+git@gmail.com>
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
 * Travel pass record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
data class NextfareTravelPassRecord(
    val version: Int,
    val timestamp: Instant,
    val checksum: Int,
) : NextfareRecord,
    Comparable<NextfareTravelPassRecord> {
    override fun compareTo(other: NextfareTravelPassRecord): Int =
        // Reverse order so highest version number is first
        other.version.compareTo(this.version)

    companion object {
        fun recordFromBytes(
            input: ByteArray,
            timeZone: TimeZone,
        ): NextfareTravelPassRecord? {
            if (NextfareRecord.byteArrayToInt(input, 2, 4) == 0) {
                // Timestamp is null, ignore.
                return null
            }

            val version = NextfareRecord.byteArrayToInt(input, 13, 1)
            if (version == 0) {
                // No travel pass loaded on this card.
                return null
            }

            return NextfareTravelPassRecord(
                version = version,
                timestamp = NextfareRecord.unpackDate(input, 2, timeZone),
                checksum = NextfareRecord.byteArrayToIntReversed(input, 14, 2),
            )
        }
    }
}
