/*
 * LaxTapTrip.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.laxtap

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.laxtap.LaxTapData.AGENCY_METRO
import com.codebutler.farebot.transit.laxtap.LaxTapData.AGENCY_SANTA_MONICA
import com.codebutler.farebot.transit.laxtap.LaxTapData.LAX_TAP_STR
import com.codebutler.farebot.transit.laxtap.LaxTapData.METRO_BUS_ROUTES
import com.codebutler.farebot.transit.laxtap.LaxTapData.METRO_BUS_START
import com.codebutler.farebot.transit.laxtap.LaxTapData.METRO_LR_START
import com.codebutler.farebot.transit.nextfare.NextfareTrip
import com.codebutler.farebot.transit.nextfare.NextfareTripCapsule
import farebot.transit.lax_tap.generated.resources.Res
import farebot.transit.lax_tap.generated.resources.lax_tap_unknown_route

/**
 * Represents trip events on LAX TAP card.
 */
class LaxTapTrip(
    capsule: NextfareTripCapsule,
) : NextfareTrip(capsule, currencyFactory = { TransitCurrency.USD(it) }) {
    override val routeName: String?
        get() {
            if (capsule.modeInt == AGENCY_METRO &&
                capsule.startStation >= METRO_BUS_START
            ) {
                // Metro Bus uses the station_id for route numbers.
                return METRO_BUS_ROUTES[capsule.startStation]
                    ?: getStringBlocking(Res.string.lax_tap_unknown_route, capsule.startStation.toString())
            }
            // Normally not possible to guess what the route is.
            return null
        }

    override val humanReadableRouteID: String?
        get() {
            if (capsule.modeInt == AGENCY_METRO &&
                capsule.startStation >= METRO_BUS_START
            ) {
                // Metro Bus uses the station_id for route numbers.
                return NumberUtils.intToHex(capsule.startStation)
            }
            // Normally not possible to guess what the route is.
            return null
        }

    override fun getStation(stationId: Int): Station? {
        if (capsule.modeInt == AGENCY_SANTA_MONICA) {
            // Santa Monica Bus doesn't use this.
            return null
        }

        if (capsule.modeInt == AGENCY_METRO && stationId >= METRO_BUS_START) {
            // Metro uses this for route names.
            return null
        }

        // Look up from MDST database
        val result = MdstStationLookup.getStation(LAX_TAP_STR, stationId) ?: return null
        return Station(
            stationNameRaw = result.stationName,
            shortStationNameRaw = result.shortStationName,
            companyName = result.companyName,
            lineNames = result.lineNames,
            latitude = if (result.hasLocation) result.latitude else null,
            longitude = if (result.hasLocation) result.longitude else null,
        )
    }

    override fun lookupMode(): Mode {
        if (capsule.modeInt == AGENCY_METRO) {
            return if (capsule.startStation >= METRO_BUS_START) {
                Mode.BUS
            } else if (capsule.startStation < METRO_LR_START && capsule.startStation != 61) {
                Mode.METRO
            } else {
                Mode.TRAM
            }
        }
        return super.lookupMode()
    }
}
