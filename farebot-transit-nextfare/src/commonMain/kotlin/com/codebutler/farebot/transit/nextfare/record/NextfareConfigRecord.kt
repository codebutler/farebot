/*
 * NextfareConfigRecord.kt
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
 * Represents a configuration record on Nextfare MFC.
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
data class NextfareConfigRecord(
    val ticketType: Int,
    val expiry: Instant,
) : NextfareRecord {
    companion object {
        fun recordFromBytes(
            input: ByteArray,
            timeZone: TimeZone,
        ): NextfareConfigRecord? {
            // Check if date bytes are all zero (no config data)
            if (NextfareRecord.byteArrayToInt(input, 4, 4) == 0) {
                return null
            }
            val expiry = NextfareRecord.unpackDate(input, 4, timeZone)
            val ticketType = NextfareRecord.byteArrayToIntReversed(input, 8, 2)
            return NextfareConfigRecord(ticketType, expiry)
        }
    }
}
