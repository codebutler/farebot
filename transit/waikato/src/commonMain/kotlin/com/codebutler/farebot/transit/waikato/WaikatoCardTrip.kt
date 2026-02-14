/*
 * WaikatoCardTrip.kt
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

package com.codebutler.farebot.transit.waikato

import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.isAllFF
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

/**
 * Represents a trip or refill event on a Waikato-region card.
 *
 * Ported from Metrodroid.
 */
class WaikatoCardTrip private constructor(
    private val timestamp: Instant,
    private val cost: Int,
    private val a: Int,
    private val b: Int,
    override val mode: Mode,
) : Trip() {
    override val startTimestamp: Instant = timestamp

    override val fare: TransitCurrency?
        get() = if (cost == 0 && mode == Mode.TICKET_MACHINE) null else TransitCurrency.NZD(cost)

    companion object {
        /**
         * Parse a 7-byte trip record from the card.
         *
         * @param sector The 7-byte trip record data.
         * @param mode The trip mode (BUS for a trip, TICKET_MACHINE for a refill).
         * @return The parsed trip, or null if the record is empty.
         */
        fun parse(
            sector: ByteArray,
            mode: Mode,
        ): WaikatoCardTrip? {
            if (sector.isAllZero() || sector.isAllFF()) return null
            val timestamp = WaikatoCardTransitFactory.parseTimestamp(sector, 1)
            val cost = sector.byteArrayToIntReversed(5, 2)
            val a = sector[0].toInt() and 0xff
            val b = sector[4].toInt() and 0xff
            return WaikatoCardTrip(
                timestamp = timestamp,
                cost = cost,
                a = a,
                b = b,
                mode = mode,
            )
        }
    }
}
