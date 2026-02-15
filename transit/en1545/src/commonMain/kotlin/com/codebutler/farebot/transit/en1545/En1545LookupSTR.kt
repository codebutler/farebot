/*
 * En1545LookupSTR.kt
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

package com.codebutler.farebot.transit.en1545

import com.codebutler.farebot.base.mdst.MdstStationTableReader
import com.codebutler.farebot.base.mdst.TransportType
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Trip
import farebot.transit.en1545.generated.resources.Res
import farebot.transit.en1545.generated.resources.en1545_unknown_format
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

/**
 * Base class for EN1545 lookups that use an MDST station table.
 */
abstract class En1545LookupSTR protected constructor(
    protected val dbName: String,
) : En1545Lookup {
    override fun getRouteName(
        routeNumber: Int?,
        routeVariant: Int?,
        agency: Int?,
        transport: Int?,
    ): String? {
        if (routeNumber == null) return null
        val routeId = routeNumber or ((agency ?: 0) shl 16) or ((transport ?: 0) shl 24)
        val reader = MdstStationTableReader.getReader(dbName) ?: return null
        val line = reader.getLine(routeId)
        return line?.name?.english ?: getHumanReadableRouteId(routeNumber, routeVariant, agency, transport)
    }

    override fun getAgencyName(
        agency: Int?,
        isShort: Boolean,
    ): FormattedString? {
        if (agency == null || agency == 0) return null
        val reader = MdstStationTableReader.getReader(dbName) ?: return null
        val operator = reader.getOperator(agency)
        val name =
            if (isShort) {
                operator?.name?.englishShort ?: operator?.name?.english
            } else {
                operator?.name?.english
            }
        return name?.let { FormattedString(it) }
    }

    override fun getStation(
        station: Int,
        agency: Int?,
        transport: Int?,
    ): Station? {
        if (station == 0) return null
        val reader = MdstStationTableReader.getReader(dbName) ?: return null
        val stationId = station or ((agency ?: 0) shl 16)
        val mdstStation = reader.getStationById(stationId)
        if (mdstStation != null) {
            val name =
                mdstStation.name.english.takeIf { it.isNotEmpty() }
                    ?: "0x${station.toString(16)}"
            val lat = mdstStation.latitude.takeIf { it != 0f }?.toString()
            val lng = mdstStation.longitude.takeIf { it != 0f }?.toString()
            return Station.create(name, null, lat, lng)
        }
        return Station.unknown("0x${station.toString(16)}")
    }

    override fun getMode(
        agency: Int?,
        route: Int?,
    ): Trip.Mode {
        if (route != null) {
            val reader = MdstStationTableReader.getReader(dbName)
            if (reader != null) {
                val lineId = if (agency != null) route or (agency shl 16) else route
                val transport = reader.getLineTransport(lineId)
                if (transport != null) return transportTypeToMode(transport)
            }
        }
        if (agency != null) {
            val reader = MdstStationTableReader.getReader(dbName)
            if (reader != null) {
                val transport = reader.getOperatorDefaultTransport(agency)
                if (transport != null) return transportTypeToMode(transport)
            }
        }
        return Trip.Mode.OTHER
    }

    override fun getSubscriptionName(
        agency: Int?,
        contractTariff: Int?,
    ): FormattedString? {
        if (contractTariff == null) return null
        val res =
            subscriptionMapByAgency[Pair(agency, contractTariff)]
                ?: subscriptionMap[contractTariff]
        return if (res != null) {
            FormattedString(res)
        } else {
            FormattedString(Res.string.en1545_unknown_format, contractTariff.toString())
        }
    }

    open val subscriptionMap: Map<Int, ComposeStringResource>
        get() = emptyMap()

    open val subscriptionMapByAgency: Map<Pair<Int?, Int>, ComposeStringResource>
        get() = emptyMap()

    companion object {
        fun transportTypeToMode(type: TransportType): Trip.Mode =
            when (type) {
                TransportType.BUS -> Trip.Mode.BUS
                TransportType.TRAIN -> Trip.Mode.TRAIN
                TransportType.TRAM -> Trip.Mode.TRAM
                TransportType.METRO -> Trip.Mode.METRO
                TransportType.FERRY -> Trip.Mode.FERRY
                TransportType.TROLLEYBUS -> Trip.Mode.TROLLEYBUS
                TransportType.MONORAIL -> Trip.Mode.MONORAIL
                else -> Trip.Mode.OTHER
            }
    }
}
