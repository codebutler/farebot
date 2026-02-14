/*
 * MobibLookup.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler
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

package com.codebutler.farebot.transit.calypso.mobib

import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.en1545.En1545LookupSTR
import farebot.farebot_transit_calypso.generated.resources.*
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

internal object MobibLookup : En1545LookupSTR("mobib") {

    private const val BUS = 0xf
    private const val TRAM = 0x16

    override val timeZone: TimeZone = TimeZone.of("Europe/Brussels")

    override fun parseCurrency(price: Int) = TransitCurrency(price, "EUR")

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? {
        if (routeNumber == null) return null
        return when (agency) {
            BUS, TRAM -> routeNumber.toString()
            else -> null
        }
    }

    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? {
        if (station == 0) return null
        return Station.unknown("0x${station.toString(16)}")
    }

    override val subscriptionMap: Map<Int, ComposeStringResource> = mapOf(
        0x2801 to Res.string.mobib_jump_1_trip,
        0x2803 to Res.string.mobib_jump_10_trips,
        0x0805 to Res.string.mobib_airport_bus,
        0x303d to Res.string.mobib_jump_24h_bus_airport
    )
}
