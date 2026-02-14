/*
 * LisboaVivaLookup.kt
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

package com.codebutler.farebot.transit.calypso.lisboaviva

import com.codebutler.farebot.base.mdst.MdstStationTableReader
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.en1545.En1545LookupSTR
import farebot.transit.calypso.generated.resources.*
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

internal object LisboaVivaLookup : En1545LookupSTR("lisboa_viva") {
    const val ZAPPING_TARIFF = 33592
    const val INTERAGENCY31_AGENCY = 31
    const val AGENCY_CARRIS = 1
    const val AGENCY_METRO = 2
    const val AGENCY_CP = 3
    const val ROUTE_CASCAIS_SADO = 40960

    override val timeZone: TimeZone = TimeZone.of("Europe/Lisbon")

    override fun parseCurrency(price: Int) = TransitCurrency(price, "EUR")

    override fun getRouteName(
        routeNumber: Int?,
        routeVariant: Int?,
        agency: Int?,
        transport: Int?,
    ): String? {
        if (routeNumber == null || routeNumber == 0) return null
        if (agency == null || agency == AGENCY_CARRIS) {
            return (routeNumber and 0xfff).toString()
        }
        val mungedRouteNumber = mungeRouteNumber(agency, routeNumber)
        val reader = MdstStationTableReader.getReader(dbName)
        val lineName = reader?.getLine(agency shl 16 or mungedRouteNumber)?.name?.english
        return lineName ?: mungedRouteNumber.toString()
    }

    override fun getHumanReadableRouteId(
        routeNumber: Int?,
        routeVariant: Int?,
        agency: Int?,
        transport: Int?,
    ): String? {
        if (routeNumber == null || agency == null) return null
        return mungeRouteNumber(agency, routeNumber).toString()
    }

    override fun getStation(
        station: Int,
        agency: Int?,
        transport: Int?,
    ): Station? {
        // transport parameter is used as routeNumber by LisboaVivaTransaction
        val routeNumber = transport
        if (station == 0 || agency == null) return null
        val mungedRouteNumber = if (routeNumber != null) mungeRouteNumber(agency, routeNumber) else 0
        val mungedStation = if (agency == AGENCY_METRO) station shr 2 else station
        val reader = MdstStationTableReader.getReader(dbName) ?: return Station.nameOnly("$agency/$station")
        val stationId = mungedStation or (mungedRouteNumber shl 8) or (agency shl 24)
        val mdstStation = reader.getStationById(stationId)
        if (mdstStation != null) {
            val name =
                mdstStation.name.english.takeIf { it.isNotEmpty() }
                    ?: "$agency/$routeNumber/$station"
            val lat = mdstStation.latitude.takeIf { it != 0f }?.toString()
            val lng = mdstStation.longitude.takeIf { it != 0f }?.toString()
            return Station.create(name, null, lat, lng)
        }
        return Station.nameOnly("$agency/$routeNumber/$station")
    }

    private fun mungeRouteNumber(
        agency: Int,
        routeNumber: Int,
    ): Int {
        if (agency == 16) return routeNumber and 0xf
        return if (agency == AGENCY_CP && routeNumber != ROUTE_CASCAIS_SADO) 4096 else routeNumber
    }

    override val subscriptionMapByAgency: Map<Pair<Int?, Int>, ComposeStringResource> =
        mapOf(
            Pair(15, 73) to Res.string.lisboa_viva_ass_pal_lis,
            Pair(15, 193) to Res.string.lisboa_viva_ass_fog_lis,
            Pair(15, 217) to Res.string.lisboa_viva_ass_pra_lis,
            Pair(16, 5) to Res.string.lisboa_viva_passe_mts,
            Pair(30, 113) to Res.string.lisboa_viva_metro_rl_12,
            Pair(30, 316) to Res.string.lisboa_viva_vermelho_a1,
            Pair(30, 454) to Res.string.lisboa_viva_metro_cp_r_mouro_melecas,
            Pair(30, 720) to Res.string.lisboa_viva_navegante_urbano,
            Pair(30, 725) to Res.string.lisboa_viva_navegante_rede,
            Pair(30, 733) to Res.string.lisboa_viva_navegante_sl_tcb_barreiro,
            Pair(30, 1088) to Res.string.lisboa_viva_fertagus_pal_lis_ml,
            Pair(INTERAGENCY31_AGENCY, 906) to Res.string.lisboa_viva_navegante_lisboa,
            Pair(INTERAGENCY31_AGENCY, ZAPPING_TARIFF) to Res.string.lisboa_viva_zapping,
        )
}
