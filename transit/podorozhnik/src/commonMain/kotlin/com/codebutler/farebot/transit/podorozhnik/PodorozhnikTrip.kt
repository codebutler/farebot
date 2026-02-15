/*
 * PodorozhnikTrip.kt
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
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import farebot.transit.podorozhnik.generated.resources.*
import kotlin.time.Instant

internal class PodorozhnikTrip(
    private val mTimestamp: Int,
    private val mFare: Int?,
    private val mLastTransport: Int,
    private val mLastValidator: Int,
) : Trip() {
    override val startTimestamp: Instant?
        get() = PodorozhnikTransitInfo.convertDate(mTimestamp)

    override val fare: TransitCurrency?
        get() =
            if (mFare != null) {
                TransitCurrency.RUB(mFare)
            } else {
                null
            }

    // TODO: Handle trams
    override val mode: Mode
        get() {
            if (mLastTransport == TRANSPORT_METRO && mLastValidator == 0) {
                return Mode.BUS
            }
            return if (mLastTransport == TRANSPORT_METRO) Mode.METRO else Mode.BUS
        }

    // TODO: handle other transports better.
    override val startStation: Station?
        get() {
            var stationId = mLastValidator or (mLastTransport shl 16)
            if (mLastTransport == TRANSPORT_METRO && mLastValidator == 0) {
                return null
            }
            if (mLastTransport == TRANSPORT_METRO) {
                val gate = stationId and 0x3f
                stationId = stationId and 0x3f.inv()
                val result = lookupMdstStation(PODOROZHNIK_STR, stationId)
                return if (result != null) {
                    result.addAttribute(FormattedString("Gate $gate"))
                } else {
                    Station.unknown((mLastValidator shr 6).toString())
                }
            }
            return lookupMdstStation(PODOROZHNIK_STR, stationId)
                ?: Station.unknown("$mLastTransport/$mLastValidator")
        }

    override val agencyName: FormattedString?
        get() =
            // Always include "Saint Petersburg" in names here to distinguish from Troika (Moscow)
            // trips on hybrid cards
            when (mLastTransport) {
                // Some validators are misconfigured and show up as Metro, station 0, gate 0.
                // Assume bus.
                TRANSPORT_METRO ->
                    if (mLastValidator == 0) {
                        FormattedString(Res.string.podorozhnik_led_bus)
                    } else {
                        FormattedString(Res.string.podorozhnik_led_metro)
                    }
                TRANSPORT_BUS, TRANSPORT_BUS_MOBILE -> FormattedString(Res.string.podorozhnik_led_bus)
                TRANSPORT_SHARED_TAXI -> FormattedString(Res.string.podorozhnik_led_shared_taxi)
                // TODO: Handle trams
                else -> FormattedString(Res.string.podorozhnik_unknown_format, mLastTransport.toString())
            }

    private fun lookupMdstStation(
        dbName: String,
        stationId: Int,
    ): Station? {
        val result = MdstStationLookup.getStation(dbName, stationId) ?: return null
        return Station
            .Builder()
            .stationName(result.stationName)
            .shortStationName(result.shortStationName)
            .companyName(result.companyName)
            .lineNames(result.lineNames)
            .latitude(if (result.hasLocation) result.latitude.toString() else null)
            .longitude(if (result.hasLocation) result.longitude.toString() else null)
            .build()
    }

    companion object {
        const val PODOROZHNIK_STR = "podorozhnik"
        const val TRANSPORT_METRO = 1

        // Some buses use fixed validators while others
        // have a mobile validator and they have different codes
        private const val TRANSPORT_BUS_MOBILE = 3
        private const val TRANSPORT_BUS = 4
        private const val TRANSPORT_SHARED_TAXI = 7
    }
}
