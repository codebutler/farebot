/*
 * AdelaideLookup.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.adelaide

import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.en1545.En1545LookupSTR
import farebot.farebot_transit_adelaide.generated.resources.Res
import farebot.farebot_transit_adelaide.generated.resources.adelaide_ticket_type_concession
import farebot.farebot_transit_adelaide.generated.resources.adelaide_ticket_type_regular
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

object AdelaideLookup : En1545LookupSTR("adelaide") {

    override val timeZone: TimeZone
        get() = TimeZone.of("Australia/Adelaide")

    override fun parseCurrency(price: Int): TransitCurrency = TransitCurrency(price, "AUD")

    internal fun isPurseTariff(agency: Int?, contractTariff: Int?): Boolean {
        if (agency == null || agency != AGENCY_ADL_METRO || contractTariff == null) {
            return false
        }
        return contractTariff in subscriptionMap
    }

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? {
        if (routeNumber == 0)
            return null
        return super.getRouteName(routeNumber, routeVariant, agency, transport)
    }

    private const val AGENCY_ADL_METRO = 1

    override val subscriptionMap: Map<Int, ComposeStringResource>
        get() = mapOf(
            0x804 to Res.string.adelaide_ticket_type_regular,
            0x808 to Res.string.adelaide_ticket_type_concession
        )
}
