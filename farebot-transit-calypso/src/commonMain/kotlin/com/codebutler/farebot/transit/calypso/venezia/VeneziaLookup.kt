/*
 * VeneziaLookup.kt
 *
 * Copyright 2018-2019 Google
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

package com.codebutler.farebot.transit.calypso.venezia

import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545LookupSTR
import farebot.farebot_transit_calypso.generated.resources.*
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

internal object VeneziaLookup : En1545LookupSTR("venezia") {
    override val timeZone: TimeZone = TimeZone.of("Europe/Rome")

    override fun parseCurrency(price: Int) = TransitCurrency(price, "EUR")

    override fun getMode(
        agency: Int?,
        route: Int?,
    ): Trip.Mode = Trip.Mode.OTHER

    override val subscriptionMap: Map<Int, ComposeStringResource> =
        mapOf(
            11105 to Res.string.venezia_24h_ticket,
            11209 to Res.string.venezia_rete_unica_75min,
            11210 to Res.string.venezia_rete_unica_100min,
            12101 to Res.string.venezia_bus_ticket_75min,
            12106 to Res.string.venezia_airport_bus_ticket,
            11400 to Res.string.venezia_carnet_traghetto,
        )
}
