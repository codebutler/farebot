/*
 * WarsawSector.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.warsaw

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.card.classic.DataClassicSector
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val TZ = TimeZone.of("Europe/Warsaw")

data class WarsawSector(
    val tripTimestamp: Instant?,
    val expiryDate: LocalDate?,
    val ticketType: Int,
    val tripType: Int,
    val counter: Int
) : Comparable<WarsawSector> {

    override operator fun compareTo(other: WarsawSector): Int = when {
        tripTimestamp == null && other.tripTimestamp == null -> 0
        tripTimestamp == null -> -1
        other.tripTimestamp == null -> 1
        tripTimestamp.compareTo(other.tripTimestamp) != 0 ->
            tripTimestamp.compareTo(other.tripTimestamp)
        else -> -((counter - other.counter) and 0xff).compareTo(0x80)
    }

    val trip: WarsawTrip?
        get() = if (tripTimestamp == null) null else WarsawTrip(tripTimestamp, tripType)

    val subscription: WarsawSubscription?
        get() {
            if (expiryDate == null) return null
            val expiry = LocalDateTime(expiryDate.year, expiryDate.month, expiryDate.day, 23, 59, 59)
            return WarsawSubscription(expiry.toInstant(TZ), ticketType)
        }

    companion object {
        fun parse(sec: DataClassicSector): WarsawSector {
            val block0 = sec.getBlock(0).data
            return WarsawSector(
                counter = block0.byteArrayToInt(1, 1),
                expiryDate = parseDate(block0, 16),
                ticketType = block0.getBitsFromBuffer(32, 12),
                tripType = block0.byteArrayToInt(9, 3),
                tripTimestamp = parseDateTime(block0, 44)
            )
        }

        private fun parseDateTime(raw: ByteArray, off: Int): Instant? {
            if (raw.getBitsFromBuffer(off, 26) == 0) return null
            val year = raw.getBitsFromBuffer(off, 6) + 2000
            val month = raw.getBitsFromBuffer(off + 6, 4)
            val day = raw.getBitsFromBuffer(off + 10, 5)
            val hour = raw.getBitsFromBuffer(off + 15, 5)
            val minute = raw.getBitsFromBuffer(off + 20, 6)
            val ldt = LocalDateTime(year, month, day, hour, minute)
            return ldt.toInstant(TZ)
        }

        private fun parseDate(raw: ByteArray, off: Int): LocalDate? {
            if (raw.getBitsFromBuffer(off, 16) == 0) return null
            val year = raw.getBitsFromBuffer(off, 7) + 2000
            val month = raw.getBitsFromBuffer(off + 7, 4)
            val day = raw.getBitsFromBuffer(off + 11, 5)
            return LocalDate(year, month, day)
        }
    }
}
