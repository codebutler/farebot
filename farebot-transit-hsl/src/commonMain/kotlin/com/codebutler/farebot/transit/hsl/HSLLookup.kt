/*
 * HSLLookup.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.hsl

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.util.getPluralStringBlocking
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.en1545.En1545LookupUnknown
import com.codebutler.farebot.transit.en1545.En1545Parsed
import farebot.farebot_transit_hsl.generated.resources.*
import kotlinx.datetime.TimeZone

object HSLLookup : En1545LookupUnknown() {
    override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)

    override val timeZone: TimeZone
        get() = TimeZone.of("Europe/Helsinki")

    fun contractWalttiZoneName(prefix: String) = "${prefix}WalttiZone"

    fun contractWalttiRegionName(prefix: String) = "${prefix}WalttiRegion"

    fun contractAreaTypeName(prefix: String) = "${prefix}AreaType"

    fun contractAreaName(prefix: String) = "${prefix}Area"

    fun languageCode(input: Int?) =
        when (input) {
            0 -> getStringBlocking(Res.string.hsl_finnish)
            1 -> getStringBlocking(Res.string.hsl_swedish)
            2 -> getStringBlocking(Res.string.hsl_english)
            else -> getStringBlocking(Res.string.hsl_unknown_format, input.toString())
        }

    private val areaMap =
        mapOf(
            Pair(0, 1) to Res.string.hsl_region_helsinki,
            Pair(0, 2) to Res.string.hsl_region_espoo,
            Pair(0, 4) to Res.string.hsl_region_vantaa,
            Pair(0, 5) to Res.string.hsl_region_seutu,
            Pair(0, 6) to Res.string.hsl_region_kirkkonummi_siuntio,
            Pair(0, 7) to Res.string.hsl_region_vihti,
            Pair(0, 8) to Res.string.hsl_region_nurmijarvi,
            Pair(0, 9) to Res.string.hsl_region_kerava_sipoo_tuusula,
            Pair(0, 10) to Res.string.hsl_region_sipoo,
            Pair(0, 14) to Res.string.hsl_region_lahiseutu_2,
            Pair(0, 15) to Res.string.hsl_region_lahiseutu_3,
            Pair(1, 1) to Res.string.hsl_transport_bussi,
            Pair(1, 2) to Res.string.hsl_transport_bussi_2,
            Pair(1, 3) to Res.string.hsl_transport_bussi_3,
            Pair(1, 4) to Res.string.hsl_transport_bussi_4,
            Pair(1, 5) to Res.string.hsl_transport_raitiovaunu,
            Pair(1, 6) to Res.string.hsl_transport_metro,
            Pair(1, 7) to Res.string.hsl_transport_juna,
            Pair(1, 8) to Res.string.hsl_transport_lautta,
            Pair(1, 9) to Res.string.hsl_transport_u_linja,
        )

    private val walttiValiditySplit =
        listOf(Pair(0, 0)) +
            (1..10).map { Pair(it, it) } +
            (1..10).flatMap { start -> ((start + 1)..10).map { Pair(start, it) } }

    private const val WALTTI_OULU = 229
    private const val WALTTI_LAHTI = 223
    const val CITY_UL_TAMPERE = 1

    private val lahtiZones =
        listOf(
            "A",
            "B",
            "C",
            "D",
            "E",
            "F1",
            "F2",
            "G",
            "H",
            "I",
        )
    private val ouluZones =
        listOf(
            "City A",
            "A",
            "B",
            "C",
            "D",
            "E",
            "F",
            "G",
            "H",
            "I",
        )

    private fun mapWalttiZone(
        region: Int,
        id: Int,
    ): String =
        when (region) {
            WALTTI_OULU -> lahtiZones[id - 1]
            WALTTI_LAHTI -> ouluZones[id - 1]
            else -> charArrayOf(('A'.code + id - 1).toChar()).concatToString()
        }

    private fun walttiNameRegion(id: Int): String? =
        MdstStationLookup.getOperatorName("waltti_region", id, isShort = true)

    fun getArea(
        parsed: En1545Parsed,
        prefix: String,
        isValidity: Boolean,
        walttiRegion: Int? = null,
        ultralightCity: Int? = null,
    ): String? {
        if (parsed.getInt(contractAreaName(prefix)) == null &&
            parsed.getInt(contractWalttiZoneName(prefix)) != null
        ) {
            val region = walttiRegion ?: parsed.getIntOrZero(contractWalttiRegionName(prefix))
            val regionName = walttiNameRegion(region) ?: region.toString()
            val zone = parsed.getIntOrZero(contractWalttiZoneName(prefix))
            if (zone == 0) {
                return null
            }
            if (!isValidity && zone in 1..10) {
                return getStringBlocking(Res.string.waltti_city_zone, regionName, mapWalttiZone(region, zone))
            }
            val (start, end) = walttiValiditySplit[zone]
            return getStringBlocking(
                Res.string.waltti_city_zones,
                regionName,
                mapWalttiZone(region, start) + " - " + mapWalttiZone(region, end),
            )
        }
        val type = parsed.getIntOrZero(contractAreaTypeName(prefix))
        val value = parsed.getIntOrZero(contractAreaName(prefix))
        if (type in 0..1 && value == 0) {
            return null
        }
        if (ultralightCity == CITY_UL_TAMPERE && type == 0) {
            val from = value % 6
            if (isValidity) {
                val to = value / 6
                val num = to - from + 1
                val zones = (from..to).map { ('A'.code + it).toChar() }.toCharArray().concatToString()
                return getPluralStringBlocking(Res.plurals.hsl_zones, num, zones)
            } else {
                return getStringBlocking(
                    Res.string.hsl_zone_station,
                    charArrayOf(('A'.code + from).toChar()).concatToString(),
                )
            }
        }
        if (type == 2) {
            val to = value and 7
            if (isValidity) {
                val from = value shr 3
                val num = to - from + 1
                val zones = (from..to).map { ('A'.code + it).toChar() }.toCharArray().concatToString()
                return getPluralStringBlocking(Res.plurals.hsl_zones, num, zones)
            } else {
                return getStringBlocking(
                    Res.string.hsl_zone_station,
                    charArrayOf(('A'.code + to).toChar()).concatToString(),
                )
            }
        }
        return areaMap[Pair(type, value)]?.let {
            getStringBlocking(it)
        } ?: getStringBlocking(Res.string.hsl_unknown_format, "$type/$value")
    }
}
