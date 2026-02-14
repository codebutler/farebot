/*
 * PisaLookup.kt
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

package com.codebutler.farebot.transit.calypso.pisa

import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545LookupSTR
import farebot.farebot_transit_calypso.generated.resources.*
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

internal object PisaLookup : En1545LookupSTR("pisa") {
    override val timeZone: TimeZone = TimeZone.of("Europe/Rome")

    override fun parseCurrency(price: Int) = TransitCurrency(price, "EUR")

    override fun getMode(
        agency: Int?,
        route: Int?,
    ): Trip.Mode = Trip.Mode.OTHER

    override val subscriptionMap: Map<Int, ComposeStringResource> =
        mapOf(
            316 to Res.string.pisa_abb_ann_pers,
            317 to Res.string.pisa_abb_mens_pers,
            322 to Res.string.pisa_carnet_10_70min,
            385 to Res.string.pisa_abb_trim_pers,
        )

    fun subscriptionUsesCounter(
        agency: Int?,
        contractTariff: Int?,
    ): Boolean = contractTariff !in listOf(316, 317, 385)
}
