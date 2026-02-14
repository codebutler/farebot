/*
 * NewShenzhenTrip.kt
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

package com.codebutler.farebot.transit.china

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Station
import farebot.farebot_transit_china.generated.resources.Res
import farebot.farebot_transit_china.generated.resources.szt_bus
import farebot.farebot_transit_china.generated.resources.szt_metro
import farebot.farebot_transit_china.generated.resources.szt_station_gate
import farebot.farebot_transit_china.generated.resources.unknown_format
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Trip implementation for Shenzhen Tong (深圳通) cards.
 *
 * Supports station lookup for metro stations and route names for buses.
 */
@Serializable
class NewShenzhenTrip(
    override val capsule: ChinaTripCapsule,
) : ChinaTripAbstract() {
    override val endStation: Station?
        get() =
            when (transport) {
                SZT_METRO -> {
                    val stationId = (mStation and 0xffffff00).toInt()
                    val result = MdstStationLookup.getStation(SHENZHEN_STR, stationId)
                    if (result != null) {
                        val gate = (mStation and 0xff).toString(16)
                        val gateAttr = getStringBlocking(Res.string.szt_station_gate, gate)
                        Station(
                            stationNameRaw = result.stationName,
                            shortStationNameRaw = result.shortStationName,
                            companyName = result.companyName,
                            lineNames = result.lineNames,
                            latitude = if (result.hasLocation) result.latitude else null,
                            longitude = if (result.hasLocation) result.longitude else null,
                            humanReadableId = (mStation shr 8).toString(16),
                            attributes = listOf(gateAttr),
                        )
                    } else {
                        Station.unknown((mStation shr 8).toString(16))
                    }
                }
                else -> null
            }

    override val mode: Mode
        get() {
            if (isTopup) {
                return Mode.TICKET_MACHINE
            }
            return when (transport) {
                SZT_METRO -> Mode.METRO
                SZT_BUS -> Mode.BUS
                else -> Mode.OTHER
            }
        }

    override val routeName: String?
        get() =
            when (transport) {
                SZT_BUS -> {
                    MdstStationLookup.getLineName(SHENZHEN_STR, mStation.toInt())
                        ?: mStation.toString()
                }
                else -> null
            }

    override val humanReadableRouteID: String?
        get() =
            when (transport) {
                SZT_BUS -> NumberUtils.intToHex(mStation.toInt())
                else -> null
            }

    override val startTimestamp: Instant?
        get() = if (transport == SZT_METRO) null else timestamp

    override val endTimestamp: Instant?
        get() = if (transport != SZT_METRO) null else timestamp

    constructor(data: ByteArray) : this(ChinaTripCapsule(data))

    override val agencyName: String?
        get() =
            when (transport) {
                SZT_METRO -> getStringBlocking(Res.string.szt_metro)
                SZT_BUS -> getStringBlocking(Res.string.szt_bus)
                else -> getStringBlocking(Res.string.unknown_format, transport.toString())
            }

    companion object {
        private const val SZT_BUS = 3
        private const val SZT_METRO = 6
        private const val SHENZHEN_STR = "shenzhen"
    }
}
