/*
 * EZLinkData.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ezlink

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.Station
import farebot.farebot_transit_ezlink.generated.resources.*

internal object EZLinkData {

    private const val EZLINK_STR = "ezlink"

    /**
     * Convert a 3-character ASCII station code to an integer for MDST lookup.
     * This matches Metrodroid's ImmutableByteArray.fromASCII(code).byteArrayToInt()
     */
    private fun codeToInt(code: String): Int {
        val bytes = code.encodeToByteArray()
        var result = 0
        for (b in bytes) {
            result = (result shl 8) or (b.toInt() and 0xFF)
        }
        return result
    }

    fun getStation(code: String): Station {
        if (code.length != 3) {
            return Station.unknown(code)
        }

        val stationId = codeToInt(code)
        val result = MdstStationLookup.getStation(EZLINK_STR, stationId)

        if (result != null) {
            return Station.Builder()
                .stationName(result.stationName)
                .shortStationName(result.shortStationName)
                .companyName(result.companyName)
                .lineName(result.lineNames.firstOrNull())
                .latitude(if (result.hasLocation) result.latitude.toString() else null)
                .longitude(if (result.hasLocation) result.longitude.toString() else null)
                .code(code)
                .build()
        }

        return Station.unknown(code)
    }

    fun getCardIssuer(canNo: String?, stringResource: StringResource): String = when (canNo?.substring(0, 3)) {
        "100" -> stringResource.getString(Res.string.ezlink_issuer_ezlink)
        "111" -> stringResource.getString(Res.string.ezlink_issuer_nets)
        else -> stringResource.getString(Res.string.ezlink_issuer_cepas)
    }
}
