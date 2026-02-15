/*
 * En1545Lookup.kt
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
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlinx.datetime.TimeZone

interface En1545Lookup {
    val timeZone: TimeZone

    fun getRouteName(
        routeNumber: Int?,
        routeVariant: Int?,
        agency: Int?,
        transport: Int?,
    ): String?

    fun getHumanReadableRouteId(
        routeNumber: Int?,
        routeVariant: Int?,
        agency: Int?,
        transport: Int?,
    ): String? {
        if (routeNumber == null) return null
        var routeReadable = "0x${routeNumber.toString(16)}"
        if (routeVariant != null) {
            routeReadable += "/0x${routeVariant.toString(16)}"
        }
        return routeReadable
    }

    fun getAgencyName(
        agency: Int?,
        isShort: Boolean,
    ): FormattedString?

    fun getStation(
        station: Int,
        agency: Int?,
        transport: Int?,
    ): Station?

    fun getSubscriptionName(
        agency: Int?,
        contractTariff: Int?,
    ): FormattedString?

    fun parseCurrency(price: Int): TransitCurrency

    fun getMode(
        agency: Int?,
        route: Int?,
    ): Trip.Mode
}
