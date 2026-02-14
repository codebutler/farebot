/*
 * CharlieCardTrip.kt
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

package com.codebutler.farebot.transit.charlie

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

class CharlieCardTrip internal constructor(
    private val mFare: Int,
    private val mValidator: Int,
    private val mTimestamp: Int,
) : Trip() {
    override val startStation: Station?
        get() = Station.unknown((mValidator shr 3).toString())

    override val startTimestamp: Instant?
        get() = CharlieCardTransitInfo.parseTimestamp(mTimestamp)

    override val fare: TransitCurrency?
        get() = TransitCurrency.USD(mFare)

    override val mode: Mode
        get() =
            when (mValidator and 7) {
                0 -> Mode.TICKET_MACHINE
                1 -> Mode.BUS
                else -> Mode.OTHER
            }

    companion object {
        fun parse(
            data: ByteArray,
            off: Int,
        ): CharlieCardTrip =
            CharlieCardTrip(
                mFare = CharlieCardTransitFactory.getPrice(data, off + 5),
                mValidator = data.byteArrayToInt(off + 3, 2),
                mTimestamp = data.byteArrayToInt(off, 3),
            )
    }
}
