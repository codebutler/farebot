/*
 * TouchnGoTrip.kt
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

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.mdst.TransportType
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.base.util.isASCII
import com.codebutler.farebot.base.util.readASCII
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

/**
 * Represents a completed Touch 'n Go transit trip (e.g., rail, bus)
 * with start and end stations.
 */
internal class TouchnGoTrip(
    private val header: ByteArray,
    private val startStationCode: TouchnGoStationId?,
    private val endStationCode: TouchnGoStationId,
) : Trip() {
    private val agencyRaw: ByteArray
        get() = header.sliceOffLen(2, 4)

    private val amount: Int
        get() = header.byteArrayToInt(10, 2)

    override val mode: Mode
        get() {
            val operatorId = agencyRaw.byteArrayToInt()
            val transportType = MdstStationLookup.getOperatorDefaultMode(TNG_STR, operatorId)
            return transportType?.toTripMode() ?: Mode.OTHER
        }

    // For completed trips, startTimestamp is not stored separately - only the end time is recorded
    override val startTimestamp: Instant? get() = null

    override val endTimestamp: Instant
        get() = parseTimestamp(header, 12)

    override val fare: TransitCurrency
        get() = TransitCurrency.MYR(amount)

    override val startStation: Station?
        get() = startStationCode?.resolve()

    override val endStation: Station
        get() = endStationCode.resolve()

    override val agencyName: FormattedString?
        get() {
            val operatorId = agencyRaw.byteArrayToInt()
            val mdstName = MdstStationLookup.getOperatorName(TNG_STR, operatorId)
            val name = mdstName ?: if (agencyRaw.isASCII()) agencyRaw.readASCII() else agencyRaw.hex()
            return FormattedString(name)
        }

    companion object {
        fun parse(sector: DataClassicSector): TouchnGoTrip? {
            if (sector.getBlock(0).isEmpty) {
                return null
            }
            return TouchnGoTrip(
                header = sector.getBlock(0).data,
                startStationCode =
                    if (isTripInProgress(sector)) {
                        null
                    } else {
                        TouchnGoStationId.parse(sector.getBlock(1).data, 6)
                    },
                endStationCode = TouchnGoStationId.parse(sector.getBlock(2).data, 6),
            )
        }
    }
}

/**
 * Maps an MDST TransportType to a FareBot Trip.Mode.
 */
internal fun TransportType.toTripMode(): Trip.Mode =
    when (this) {
        TransportType.BUS -> Trip.Mode.BUS
        TransportType.TRAIN -> Trip.Mode.TRAIN
        TransportType.TRAM -> Trip.Mode.TRAM
        TransportType.METRO -> Trip.Mode.METRO
        TransportType.FERRY -> Trip.Mode.FERRY
        TransportType.TICKET_MACHINE -> Trip.Mode.TICKET_MACHINE
        TransportType.VENDING_MACHINE -> Trip.Mode.VENDING_MACHINE
        TransportType.POS -> Trip.Mode.POS
        TransportType.TROLLEYBUS -> Trip.Mode.TROLLEYBUS
        TransportType.TOLL_ROAD -> Trip.Mode.TOLL_ROAD
        TransportType.MONORAIL -> Trip.Mode.MONORAIL
        TransportType.BANNED -> Trip.Mode.BANNED
        else -> Trip.Mode.OTHER
    }
