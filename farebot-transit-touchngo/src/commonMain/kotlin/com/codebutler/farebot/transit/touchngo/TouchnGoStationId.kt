/*
 * TouchnGoStationId.kt
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
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Station
import farebot.farebot_transit_touchngo.generated.resources.Res
import farebot.farebot_transit_touchngo.generated.resources.touchngo_machine

/**
 * Represents a station identifier on a Touch 'n Go card, consisting of
 * a station code and a machine code.
 */
internal data class TouchnGoStationId(
    val station: Int,
    val machine: Int,
) {
    /**
     * Resolves this station ID to a [Station] using the MDST database,
     * adding the machine number as an attribute.
     */
    fun resolve(): Station {
        val result = MdstStationLookup.getStation(TNG_STR, station)
        val baseStation =
            if (result != null) {
                Station(
                    stationNameRaw = result.stationName,
                    shortStationNameRaw = result.shortStationName,
                    companyName = result.companyName,
                    lineNames = result.lineNames,
                    latitude = if (result.hasLocation) result.latitude else null,
                    longitude = if (result.hasLocation) result.longitude else null,
                )
            } else {
                Station.unknown(station.toString())
            }
        val machineAttr = getStringBlocking(Res.string.touchngo_machine, machine)
        return baseStation.addAttribute(machineAttr)
    }

    companion object {
        fun parse(
            raw: ByteArray,
            off: Int,
        ): TouchnGoStationId =
            TouchnGoStationId(
                station = raw.byteArrayToInt(off, 2),
                machine = raw.byteArrayToInt(off + 2, 2),
            )
    }
}
