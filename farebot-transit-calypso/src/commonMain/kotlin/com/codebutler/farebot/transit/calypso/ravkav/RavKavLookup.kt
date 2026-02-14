/*
 * RavKavLookup.kt
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

package com.codebutler.farebot.transit.calypso.ravkav

import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545LookupSTR
import farebot.farebot_transit_calypso.generated.resources.Res
import farebot.farebot_transit_calypso.generated.resources.ravkav_generic_trips
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

object RavKavLookup : En1545LookupSTR("ravkav") {
    override val timeZone: TimeZone = TimeZone.of("Asia/Jerusalem")

    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? {
        return if (station == 0) null else Station.unknown(station.toString())
    }

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? {
        if (routeNumber == null || routeNumber == 0) return null
        return if (agency == 3) {
            // Egged
            (routeNumber % 1000).toString()
        } else {
            routeNumber.toString()
        }
    }

    override fun getMode(agency: Int?, route: Int?): Trip.Mode {
        return Trip.Mode.OTHER
    }

    override fun parseCurrency(price: Int) = TransitCurrency(price, "ILS")

    override val subscriptionMap: Map<Int, ComposeStringResource> = mapOf(
        641 to Res.string.ravkav_generic_trips
    )
}
