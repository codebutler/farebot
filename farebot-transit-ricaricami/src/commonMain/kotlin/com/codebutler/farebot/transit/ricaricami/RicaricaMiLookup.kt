/*
 * RicaricaMiLookup.kt
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

package com.codebutler.farebot.transit.ricaricami

import com.codebutler.farebot.base.mdst.MdstStationTableReader
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.en1545.En1545LookupSTR
import farebot.farebot_transit_ricaricami.generated.resources.Res
import farebot.farebot_transit_ricaricami.generated.resources.ricaricami_daily_urban
import farebot.farebot_transit_ricaricami.generated.resources.ricaricami_m1_3_ord_single
import farebot.farebot_transit_ricaricami.generated.resources.ricaricami_monthly_urban
import farebot.farebot_transit_ricaricami.generated.resources.ricaricami_single_urban
import farebot.farebot_transit_ricaricami.generated.resources.ricaricami_urban_2x6
import farebot.farebot_transit_ricaricami.generated.resources.ricaricami_yearly_urban
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

object RicaricaMiLookup : En1545LookupSTR("ricaricami") {
    override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)

    override val timeZone: TimeZone get() = TZ

    override fun getStation(
        station: Int,
        agency: Int?,
        transport: Int?,
    ): Station? {
        if (station == 0) {
            return null
        }
        val reader = MdstStationTableReader.getReader(dbName) ?: return null
        val stationId = station or ((transport ?: 0) shl 24)
        val mdstStation = reader.getStationById(stationId)
        if (mdstStation != null) {
            val name =
                mdstStation.name.english.takeIf { it.isNotEmpty() }
                    ?: NumberUtils.intToHex(station)
            val lat = mdstStation.latitude.takeIf { it != 0f }?.toString()
            val lng = mdstStation.longitude.takeIf { it != 0f }?.toString()
            return Station.create(name, null, lat, lng)
        }
        return Station.nameOnly(NumberUtils.intToHex(station))
    }

    override fun getRouteName(
        routeNumber: Int?,
        routeVariant: Int?,
        agency: Int?,
        transport: Int?,
    ): String? {
        if (routeNumber == null) {
            return null
        }
        when (transport) {
            TRANSPORT_METRO -> {
                when (routeNumber) {
                    101 -> return "M1"
                    104 -> return "M2"
                    107 -> return "M5"
                    301 -> return "M3"
                }
            }
            TRANSPORT_TRENORD1, TRANSPORT_TRENORD2 -> {
                // Essentially a placeholder
                if (routeNumber == 1000) {
                    return null
                }
            }
            TRANSPORT_TRAM -> {
                if (routeNumber == 60) {
                    return null
                }
            }
        }
        if (routeVariant != null) {
            return "$routeNumber/$routeVariant"
        }
        return routeNumber.toString()
    }

    val TZ: TimeZone = TimeZone.of("Europe/Rome")
    const val TRANSPORT_METRO = 1
    const val TRANSPORT_BUS = 2
    const val TRANSPORT_TRAM = 4
    const val TRANSPORT_TRENORD1 = 7
    const val TRANSPORT_TRENORD2 = 9
    const val TARIFF_URBAN_2X6 = 0x1b39
    const val TARIFF_SINGLE_URBAN = 0xfff
    const val TARIFF_DAILY_URBAN = 0x100d
    const val TARIFF_YEARLY_URBAN = 45
    const val TARIFF_MONTHLY_URBAN = 46

    override val subscriptionMap: Map<Int, ComposeStringResource> =
        mapOf(
            TARIFF_SINGLE_URBAN to Res.string.ricaricami_single_urban,
            TARIFF_DAILY_URBAN to Res.string.ricaricami_daily_urban,
            TARIFF_URBAN_2X6 to Res.string.ricaricami_urban_2x6,
            TARIFF_YEARLY_URBAN to Res.string.ricaricami_yearly_urban,
            TARIFF_MONTHLY_URBAN to Res.string.ricaricami_monthly_urban,
            7095 to Res.string.ricaricami_m1_3_ord_single,
        )
}
