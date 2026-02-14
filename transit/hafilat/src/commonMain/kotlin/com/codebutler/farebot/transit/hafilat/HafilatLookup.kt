/*
 * HafilatLookup.kt
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

package com.codebutler.farebot.transit.hafilat

import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.en1545.En1545LookupSTR
import farebot.transit.hafilat.generated.resources.Res
import farebot.transit.hafilat.generated.resources.hafilat_subscription_regular
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

object HafilatLookup : En1545LookupSTR("hafilat") {
    override val timeZone: TimeZone
        get() = TimeZone.of("Asia/Dubai")

    override fun parseCurrency(price: Int): TransitCurrency = TransitCurrency(price, "AED")

    internal fun isPurseTariff(
        agency: Int?,
        contractTariff: Int?,
    ): Boolean = agency == 1 && contractTariff in listOf(0x2710)

    override fun getRouteName(
        routeNumber: Int?,
        routeVariant: Int?,
        agency: Int?,
        transport: Int?,
    ): String? = routeNumber?.toString()

    override val subscriptionMap: Map<Int, ComposeStringResource>
        get() =
            mapOf(
                0x2710 to Res.string.hafilat_subscription_regular,
            )
}
