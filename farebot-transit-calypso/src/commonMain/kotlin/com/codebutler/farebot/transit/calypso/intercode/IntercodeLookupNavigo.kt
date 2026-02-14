/*
 * IntercodeLookupNavigo.kt
 *
 * Copyright 2009-2013 by 'L1L1'
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

package com.codebutler.farebot.transit.calypso.intercode

import com.codebutler.farebot.base.mdst.MdstStationTableReader
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Transaction
import com.codebutler.farebot.transit.en1545.En1545TransitData
import farebot.farebot_transit_calypso.generated.resources.*
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

private const val NAVIGO_STR = "navigo"

internal object IntercodeLookupNavigo : IntercodeLookupSTR(NAVIGO_STR) {
    override fun cardName(env: () -> En1545Parsed): String =
        if (env().getIntOrZero(En1545TransitData.HOLDER_CARD_TYPE) == 1)
            NAVIGO_DECOUVERTE_NAME
        else
            NAVIGO_NAME

    override val allCardNames: List<String>
        get() = listOf(NAVIGO_NAME)

    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? {
        if (station == 0)
            return null
        var mdstStationId = station or ((agency ?: 0) shl 16) or ((transport ?: 0) shl 24)
        val sectorId = station shr 9
        val stationId = station shr 4 and 0x1F
        var humanReadableId = station.toString()
        var fallBackName = station.toString()
        if (transport == En1545Transaction.TRANSPORT_TRAIN && (agency == RATP || agency == SNCF)) {
            mdstStationId = mdstStationId and -0xff0010 or 0x30000
        }
        if ((agency == RATP || agency == SNCF) && (transport == En1545Transaction.TRANSPORT_METRO || transport == En1545Transaction.TRANSPORT_TRAM)) {
            mdstStationId = mdstStationId and 0x0000fff0 or 0x3020000
            fallBackName = if (SECTOR_NAMES[sectorId] != null)
                "${SECTOR_NAMES[sectorId]} #$stationId"
            else
                getStringBlocking(Res.string.navigo_sector_station, sectorId, stationId)
            humanReadableId = "$sectorId/$stationId"
        }

        val reader = MdstStationTableReader.getReader(NAVIGO_STR)
        if (reader != null) {
            val mdstStation = reader.getStationById(mdstStationId)
            if (mdstStation != null) {
                val name = mdstStation.name.english.takeIf { it.isNotEmpty() }
                    ?: fallBackName
                val lat = mdstStation.latitude.takeIf { it != 0f }?.toString()
                val lng = mdstStation.longitude.takeIf { it != 0f }?.toString()
                return Station.create(name, null, lat, lng)
            }
        }
        return Station.unknown(fallBackName)
    }

    override val subscriptionMap: Map<Int, ComposeStringResource> = mapOf(
        0 to Res.string.navigo_forfait_mois,
        1 to Res.string.navigo_forfait_semaine,
        2 to Res.string.navigo_forfait_annuel,
        3 to Res.string.navigo_forfait_jour,
        5 to Res.string.navigo_forfait_imagineR_etudiant,
        4096 to Res.string.navigo_forfait_liberte,
        16384 to Res.string.navigo_forfait_mois_75,
        16385 to Res.string.navigo_forfait_semaine_75,
        20480 to Res.string.navigo_ticket_tplus,
        20488 to Res.string.navigo_metro_train_rer,
        32771 to Res.string.navigo_forfait_solidarite_gratuite,
    )

    private val SECTOR_NAMES = mapOf(
        1 to "Cité",
        2 to "Rennes",
        3 to "Villette",
        4 to "Montparnasse",
        5 to "Nation",
        6 to "Saint-Lazare",
        7 to "Auteuil",
        8 to "République",
        9 to "Austerlitz",
        10 to "Invalides",
        11 to "Sentier",
        12 to "Île Saint-Louis",
        13 to "Daumesnil",
        14 to "Italie",
        15 to "Denfert",
        16 to "Félix Faure",
        17 to "Passy",
        18 to "Étoile",
        19 to "Clichy - Saint Ouen",
        20 to "Montmartre",
        21 to "Lafayette",
        22 to "Buttes Chaumont",
        23 to "Belleville",
        24 to "Père Lachaise",
        25 to "Charenton",
        26 to "Ivry - Villejuif",
        27 to "Vanves",
        28 to "Issy",
        29 to "Levallois",
        30 to "Péreire",
        31 to "Pigalle"
    )

    private const val RATP = 3
    private const val SNCF = 2

    private const val NAVIGO_NAME = "Navigo"
    private const val NAVIGO_DECOUVERTE_NAME = "Navigo découverte"

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? {
        if (agency == RATP && routeNumber != null) {
            val reader = MdstStationTableReader.getReader(NAVIGO_STR)
            if (reader != null) {
                val line = reader.getLine(routeNumber)
                if (line?.name?.english != null) return line.name.english
            }
        }
        return super.getRouteName(routeNumber, routeNumber, agency, transport)
    }
}
