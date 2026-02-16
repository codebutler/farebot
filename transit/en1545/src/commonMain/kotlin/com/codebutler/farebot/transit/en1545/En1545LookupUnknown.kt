/*
 * En1545LookupUnknown.kt
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

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Trip

abstract class En1545LookupUnknown : En1545Lookup {
    override fun getRouteName(
        routeNumber: Int?,
        routeVariant: Int?,
        agency: Int?,
        transport: Int?,
    ): String? {
        if (routeNumber == null) return null
        var routeReadable = routeNumber.toString()
        if (routeVariant != null) {
            routeReadable += "/$routeVariant"
        }
        return routeReadable
    }

    override fun getAgencyName(
        agency: Int?,
        isShort: Boolean,
    ): FormattedString? = if (agency == null || agency == 0) null else FormattedString(agency.toString())

    override fun getStation(
        station: Int,
        agency: Int?,
        transport: Int?,
    ): Station? = if (station == 0) null else Station.unknown(station.toString())

    override fun getSubscriptionName(
        agency: Int?,
        contractTariff: Int?,
    ): FormattedString? = contractTariff?.let { FormattedString(it.toString()) }

    override fun getMode(
        agency: Int?,
        route: Int?,
    ): Trip.Mode = Trip.Mode.OTHER
}
