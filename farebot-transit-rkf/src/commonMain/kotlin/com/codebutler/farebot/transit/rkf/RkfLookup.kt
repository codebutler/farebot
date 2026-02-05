/*
 * RkfLookup.kt
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

package com.codebutler.farebot.transit.rkf

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.hexString
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545LookupSTR
import farebot.farebot_transit_rkf.generated.resources.Res
import farebot.farebot_transit_rkf.generated.resources.rkf_stockholm_30_days
import farebot.farebot_transit_rkf.generated.resources.rkf_stockholm_72_hours
import farebot.farebot_transit_rkf.generated.resources.rkf_stockholm_7_days
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource

private const val STR = "rkf"

data class RkfLookup(val mCurrencyCode: Int, val mCompany: Int) : En1545LookupSTR(STR) {
    override fun parseCurrency(price: Int): TransitCurrency {
        val intendedDivisor = when (mCurrencyCode shr 12) {
            0 -> 1
            1 -> 10
            2 -> 100
            9 -> 2
            else -> 1
        }

        val numericCode = NumberUtils.convertBCDtoInteger(mCurrencyCode and 0xfff)
        val currencyString = iso4217NumericToAlpha(numericCode)
        return TransitCurrency(price, currencyString, intendedDivisor)
    }

    override val timeZone: TimeZone get() = when (mCompany / 1000) {
        // FIXME: mCompany is an AID from the TCCI, and these are special values that aren't used?
        0 -> TimeZone.of("Europe/Stockholm")
        1 -> TimeZone.of("Europe/Oslo")
        2 -> TimeZone.of("Europe/Copenhagen")

        // Per RKF-0019 "Table of Existing Application Identifiers"
        // 064 - 1f3: Swedish public transport authorisations acting on county or municipal levels
        // 1f4 - 3e7: Swedish public transport authorisations acting on national or regional levels
        //            and other operators
        in 0x64..0x3e7 -> TimeZone.of("Europe/Stockholm")

        // Norwegian public transport authorisations or other operators
        in 0x3e8..0x7cf -> TimeZone.of("Europe/Oslo")

        // Danish public transport authorisations or other operators
        in 0x7d0..0xbb7 -> TimeZone.of("Europe/Copenhagen")

        // Fallback
        else -> TimeZone.of("Europe/Stockholm")
    }

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? {
        if (routeNumber == null)
            return null
        val routeId = routeNumber or ((agency ?: 0) shl 16)
        val routeReadable = getHumanReadableRouteId(routeNumber, routeVariant, agency, transport)
        return super.getRouteName(routeNumber, routeVariant, agency, transport)
            ?: routeReadable
    }

    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? {
        if (station == 0)
            return null
        val stationId = station or ((agency ?: 0) shl 16)
        val humanReadable = (if (agency != null) "${agency.hexString}/" else "") +
                station.hexString
        return super.getStation(station, agency, transport)
            ?: Station.nameOnly(humanReadable)
    }

    override val subscriptionMapByAgency: Map<Pair<Int?, Int>, StringResource> get() = mapOf(
        Pair(SLACCESS, 1022) to Res.string.rkf_stockholm_30_days,
        Pair(SLACCESS, 1184) to Res.string.rkf_stockholm_7_days,
        Pair(SLACCESS, 1225) to Res.string.rkf_stockholm_72_hours
    )

    companion object {
        const val SLACCESS = 101
        const val VASTTRAFIK = 240
        const val REJSEKORT = 2000

        /**
         * Convert ISO 4217 numeric currency code to 3-letter alpha code.
         * Covers the Scandinavian currencies used in RKF systems.
         */
        private fun iso4217NumericToAlpha(numericCode: Int): String = when (numericCode) {
            208 -> "DKK"  // Danish Krone
            578 -> "NOK"  // Norwegian Krone
            752 -> "SEK"  // Swedish Krona
            978 -> "EUR"  // Euro
            840 -> "USD"  // US Dollar
            826 -> "GBP"  // British Pound
            756 -> "CHF"  // Swiss Franc
            else -> "XXX" // Unknown
        }
    }
}
