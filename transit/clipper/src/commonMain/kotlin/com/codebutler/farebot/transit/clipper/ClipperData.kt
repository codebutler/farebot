/*
 * ClipperData.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2014 Bao-Long Nguyen-Trong <baolong@inkling.com>
 * Copyright (C) 2018 Google
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

package com.codebutler.farebot.transit.clipper

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.mdst.TransportType
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Trip
import farebot.transit.clipper.generated.resources.Res
import farebot.transit.clipper.generated.resources.clipper_unknown_agency

internal object ClipperData {
    private const val CLIPPER_STR = "clipper"

    const val AGENCY_ACTRAN = 0x01
    const val AGENCY_BART = 0x04
    const val AGENCY_CALTRAIN = 0x06
    const val AGENCY_CCTA = 0x08
    const val AGENCY_GGT = 0x0b
    const val AGENCY_SMART = 0x0c
    const val AGENCY_SAMTRANS = 0x0f
    const val AGENCY_VTA = 0x11
    const val AGENCY_MUNI = 0x12
    const val AGENCY_GG_FERRY = 0x19
    const val AGENCY_SF_BAY_FERRY = 0x1b
    const val AGENCY_CALTRAIN_8RIDE = 0x173

    fun getAgencyName(agency: Int): FormattedString {
        val result = MdstStationLookup.getOperatorName(CLIPPER_STR, agency)
        if (result != null) return FormattedString(result)
        return FormattedString(Res.string.clipper_unknown_agency, agency.toString(16))
    }

    fun getShortAgencyName(agency: Int): FormattedString {
        val result = MdstStationLookup.getOperatorName(CLIPPER_STR, agency, isShort = true)
        if (result != null) return FormattedString(result)
        return FormattedString(Res.string.clipper_unknown_agency, agency.toString(16))
    }

    fun getStation(
        agency: Int,
        stationId: Int,
        isEnd: Boolean,
    ): Station? {
        val id = (agency shl 16) or stationId
        val result = MdstStationLookup.getStation(CLIPPER_STR, id)
        if (result != null) {
            return Station(
                stationName = result.stationName,
                shortStationName = result.shortStationName,
                companyName = result.companyName,
                lineNames = result.lineNames,
                latitude = if (result.hasLocation) result.latitude else null,
                longitude = if (result.hasLocation) result.longitude else null,
            )
        }

        if (agency == AGENCY_GGT ||
            agency == AGENCY_CALTRAIN ||
            agency == AGENCY_GG_FERRY ||
            agency == AGENCY_SMART
        ) {
            if (stationId == 0xffff) {
                return Station.nameOnly("(End of line)")
            }
            if (agency != AGENCY_GG_FERRY) {
                return Station.nameOnly("Zone $stationId")
            }
        }

        // Placeholders
        if (stationId == (if (isEnd) 0xffff else 0)) return null
        return Station.nameOnly(
            "0x${agency.toString(16)}/0x${stationId.toString(16)}",
        )
    }

    fun getRouteName(
        agency: Int,
        routeId: Int,
    ): String? {
        val id = (agency shl 16) or routeId
        val result = MdstStationLookup.getLineName(CLIPPER_STR, id)
        return result
    }

    fun getMode(
        agency: Int,
        transportCode: Int,
    ): Trip.Mode =
        when (transportCode) {
            0x62 -> {
                when (agency) {
                    AGENCY_SF_BAY_FERRY, AGENCY_GG_FERRY -> Trip.Mode.FERRY
                    AGENCY_CALTRAIN, AGENCY_SMART -> Trip.Mode.TRAIN
                    else -> Trip.Mode.TRAM
                }
            }
            0x6f -> Trip.Mode.METRO
            0x61, 0x75 -> Trip.Mode.BUS
            0x73 -> Trip.Mode.FERRY
            0x77 -> Trip.Mode.BUS
            0x78 -> Trip.Mode.TRAIN
            else -> Trip.Mode.OTHER
        }

    /**
     * Get the default mode for an agency from MDST operator data.
     * Used by ClipperUltralightTrip where transport code is not available.
     */
    fun getMode(agency: Int): Trip.Mode {
        val transportType = MdstStationLookup.getOperatorDefaultMode(CLIPPER_STR, agency)
        return transportType?.toTripMode() ?: Trip.Mode.OTHER
    }

    private fun TransportType.toTripMode(): Trip.Mode =
        when (this) {
            TransportType.BUS -> Trip.Mode.BUS
            TransportType.TRAIN -> Trip.Mode.TRAIN
            TransportType.TRAM -> Trip.Mode.TRAM
            TransportType.METRO -> Trip.Mode.METRO
            TransportType.FERRY -> Trip.Mode.FERRY
            TransportType.TICKET_MACHINE -> Trip.Mode.TICKET_MACHINE
            TransportType.VENDING_MACHINE -> Trip.Mode.VENDING_MACHINE
            TransportType.POS -> Trip.Mode.POS
            TransportType.BANNED -> Trip.Mode.BANNED
            TransportType.TROLLEYBUS -> Trip.Mode.TROLLEYBUS
            TransportType.TOLL_ROAD -> Trip.Mode.TOLL_ROAD
            TransportType.MONORAIL -> Trip.Mode.MONORAIL
            TransportType.UNKNOWN, TransportType.OTHER -> Trip.Mode.OTHER
        }
}
