/*
 * LeapTripPoint.kt
 *
 * Copyright 2018-2019 Google
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

package com.codebutler.farebot.transit.tfileap

import kotlin.time.Instant

internal fun <T> valuesCompatible(
    a: T?,
    b: T?,
): Boolean = (a == null || b == null || a == b)

internal class LeapTripPoint(
    val timestamp: Instant?,
    val amount: Int?,
    private val eventCode: Int?,
    val station: Int?,
) {
    fun isMergeable(other: LeapTripPoint?): Boolean =
        other == null ||
            (
                valuesCompatible(amount, other.amount) &&
                    valuesCompatible(timestamp, other.timestamp) &&
                    valuesCompatible(eventCode, other.eventCode) &&
                    valuesCompatible(station, other.station)
            )

    companion object {
        fun merge(
            a: LeapTripPoint?,
            b: LeapTripPoint?,
        ) = LeapTripPoint(
            timestamp = a?.timestamp ?: b?.timestamp,
            amount = a?.amount ?: b?.amount,
            eventCode = a?.eventCode ?: b?.eventCode,
            station = a?.station ?: b?.station,
        )
    }
}
