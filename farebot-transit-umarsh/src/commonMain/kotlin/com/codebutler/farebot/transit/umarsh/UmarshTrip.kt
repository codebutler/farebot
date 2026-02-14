/*
 * UmarshTrip.kt
 *
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

package com.codebutler.farebot.transit.umarsh

import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.readASCII
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.zolotayakorona.RussiaTaxCodes
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.time.Instant

class UmarshTrip(
    private val timestamp: Instant,
    private val routeNameValue: String,
    private val vehicleIDValue: String,
    private val transportType: Int,
) : Trip() {
    override val startTimestamp: Instant get() = timestamp

    override val routeName: String get() = routeNameValue

    override val vehicleID: String get() = vehicleIDValue

    override val fare: TransitCurrency? get() = null

    override val mode: Mode
        get() =
            when (transportType) {
                1 -> Mode.BUS
                2 -> Mode.TROLLEYBUS
                3 -> Mode.TRAM
                4 -> Mode.BUS
                5 -> Mode.BUS
                else -> Mode.OTHER
            }

    companion object {
        fun parse(
            raw: ByteArray,
            region: Int,
        ): UmarshTrip? {
            val trans = raw.getBitsFromBuffer(0, 3)
            val tm = raw.getBitsFromBuffer(3, 13)
            val date = parseDate(raw, 16) ?: return null
            val route = raw.sliceOffLen(4, 6).readASCII()
            val veh = raw.sliceOffLen(10, 6).readASCII()
            val tz = RussiaTaxCodes.codeToTimeZone(region)
            val ldt = LocalDateTime(date.year, date.month, date.day, tm / 100, tm % 100)
            val instant = ldt.toInstant(tz)
            return UmarshTrip(
                timestamp = instant,
                routeNameValue = route,
                vehicleIDValue = veh,
                transportType = trans,
            )
        }
    }
}

internal fun parseDate(
    raw: ByteArray,
    off: Int,
): LocalDate? {
    val rawBits = raw.getBitsFromBuffer(off, 16)
    if (rawBits == 0 || rawBits == 0xffff) return null
    val year = raw.getBitsFromBuffer(off, 7) + 2000
    val month = raw.getBitsFromBuffer(off + 7, 4)
    val day = raw.getBitsFromBuffer(off + 11, 5)
    return LocalDate(year, month, day)
}
