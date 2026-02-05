/*
 * PodorozhnikTopup.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.podorozhnik

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_podorozhnik.generated.resources.*
import kotlin.time.Instant

internal class PodorozhnikTopup(
    private val mTimestamp: Int,
    private val mFare: Int,
    private val mAgency: Int,
    private val mTopupMachine: Int,
    private val stringResource: StringResource,
) : Trip() {

    override val startTimestamp: Instant?
        get() = PodorozhnikTransitInfo.convertDate(mTimestamp)

    override val fare: TransitCurrency?
        get() = TransitCurrency.RUB(-mFare)

    override val mode: Mode
        get() = Mode.TICKET_MACHINE

    override val machineID: String?
        get() = mTopupMachine.toString()

    // TODO: handle other transports better.
    override val startStation: Station?
        get() {
            if (mAgency == PodorozhnikTrip.TRANSPORT_METRO) {
                val station = mTopupMachine / 10
                val stationId = (PodorozhnikTrip.TRANSPORT_METRO shl 16) or (station shl 6)
                return lookupMdstStation(PodorozhnikTrip.PODOROZHNIK_STR, stationId)
                    ?: Station.unknown(station.toString())
            }
            return Station.unknown(mAgency.toString(16) + "/" + mTopupMachine.toString(16))
        }

    override val agencyName: String?
        get() = when (mAgency) {
            1 -> stringResource.getString(Res.string.podorozhnik_topup)
            else -> stringResource.getString(Res.string.podorozhnik_unknown_format, mAgency.toString())
        }

    private fun lookupMdstStation(dbName: String, stationId: Int): Station? {
        val result = MdstStationLookup.getStation(dbName, stationId) ?: return null
        return Station.Builder()
            .stationName(result.stationName)
            .shortStationName(result.shortStationName)
            .companyName(result.companyName)
            .lineName(result.lineNames.firstOrNull())
            .latitude(if (result.hasLocation) result.latitude.toString() else null)
            .longitude(if (result.hasLocation) result.longitude.toString() else null)
            .build()
    }
}
