/*
 * OvcLookup.kt
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

package com.codebutler.farebot.transit.ovc

import com.codebutler.farebot.base.mdst.MdstStationTableReader
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.en1545.En1545LookupSTR
import farebot.farebot_transit_ovc.generated.resources.*
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource

private const val OVCHIP_STR = "ovc"

object OvcLookup : En1545LookupSTR(OVCHIP_STR) {
    override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)

    override val timeZone get() = TimeZone.of("Europe/Amsterdam")

    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? {
        if (agency == null)
            return Station.unknown(station.toString())
        val companyCodeShort = agency and 0xFFFF

        // TLS is the OVChip operator, and doesn't have any stations.
        if (companyCodeShort == 0) return null

        val stationId = (companyCodeShort - 1 shl 16) or (station and 0xFFFF)
        if (stationId <= 0) return null
        val reader = MdstStationTableReader.getReader(OVCHIP_STR) ?: return null
        val mdstStation = reader.getStationById(stationId) ?: return null
        val name = mdstStation.name.english.takeIf { it.isNotEmpty() }
            ?: "0x${station.toString(16)}"
        val lat = mdstStation.latitude.takeIf { it != 0f }?.toString()
        val lng = mdstStation.longitude.takeIf { it != 0f }?.toString()
        return Station.create(name, null, lat, lng)
    }

    override val subscriptionMap: Map<Int, StringResource> = mapOf(
        /* It seems that all the IDs are unique, so why bother with the companies? */
        /* NS */
        0x0005 to Res.string.ovc_sub_ov_jaarkaart,
        0x0007 to Res.string.ovc_sub_ov_bijkaart_1e_klas,
        0x0011 to Res.string.ovc_sub_ns_businesscard,
        0x0019 to Res.string.ovc_sub_voordeelurenabonnement_twee_jaar,
        0x00af to Res.string.ovc_sub_studenten_ov_chipkaart_week_2009,
        0x00b0 to Res.string.ovc_sub_studenten_ov_chipkaart_weekend_2009,
        0x00b1 to Res.string.ovc_sub_studentenkaart_korting_week_2009,
        0x00b2 to Res.string.ovc_sub_studentenkaart_korting_weekend_2009,
        0x00c9 to Res.string.ovc_sub_reizen_op_saldo_bij_ns_1e_klasse,
        0x00ca to Res.string.ovc_sub_reizen_op_saldo_bij_ns_2de_klasse,
        0x00ce to Res.string.ovc_sub_voordeelurenabonnement_reizen_op_saldo,
        0x00e5 to Res.string.ovc_sub_reizen_op_saldo_tijdelijk_eerste_klas,
        0x00e6 to Res.string.ovc_sub_reizen_op_saldo_tijdelijk_tweede_klas,
        0x00e7 to Res.string.ovc_sub_reizen_op_saldo_tijdelijk_eerste_klas_korting,
        0x0226 to Res.string.ovc_sub_reizen_op_rekening_trein,
        0x0227 to Res.string.ovc_sub_reizen_op_rekening_bus_tram_metro,
        /* Arriva */
        0x059a to Res.string.ovc_sub_dalkorting,
        /* Veolia */
        0x0626 to Res.string.ovc_sub_dalu_dalkorting,
        /* GVB */
        0x0675 to Res.string.ovc_sub_gvb_nachtbus_saldo,
        /* Connexxion */
        0x0692 to Res.string.ovc_sub_daluren_oost_nederland,
        0x069c to Res.string.ovc_sub_daluren_oost_nederland,
        /* DUO */
        0x09c6 to Res.string.ovc_sub_student_weekend_vrij,
        0x09c7 to Res.string.ovc_sub_student_week_korting,
        0x09c9 to Res.string.ovc_sub_student_week_vrij,
        0x09ca to Res.string.ovc_sub_student_weekend_korting,
        /* GVB (continued) */
        0x0bbd to Res.string.ovc_sub_fietssupplement)
}
