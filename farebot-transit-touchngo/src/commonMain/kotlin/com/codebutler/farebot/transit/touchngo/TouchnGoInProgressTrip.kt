/*
 * TouchnGoInProgressTrip.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
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

package com.codebutler.farebot.transit.touchngo

import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

/**
 * Represents a Touch 'n Go trip that is currently in progress (tapped on but not yet tapped off).
 */
internal class TouchnGoInProgressTrip(
    override val startTimestamp: Instant,
    private val startStationCode: TouchnGoStationId,
    private val agencyRawShort: ByteArray
) : Trip() {

    override val fare: TransitCurrency? get() = null

    override val mode: Mode get() = Mode.OTHER

    override val startStation: Station
        get() = startStationCode.resolve()

    companion object {
        fun parse(sector: DataClassicSector): TouchnGoInProgressTrip? {
            if (!isTripInProgress(sector)) {
                return null
            }
            val blk = sector.getBlock(1).data
            return TouchnGoInProgressTrip(
                agencyRawShort = blk.copyOfRange(0, 2),
                startTimestamp = parseTimestamp(blk, 2),
                startStationCode = TouchnGoStationId.parse(blk, 6)
            )
        }
    }
}
