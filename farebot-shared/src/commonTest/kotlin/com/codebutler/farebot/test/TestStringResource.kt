/*
 * TestStringResource.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2024 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.test

import com.codebutler.farebot.base.util.StringResource
import org.jetbrains.compose.resources.StringResource as ComposeStringResource

/**
 * Test implementation of StringResource that returns known strings or the resource key name.
 */
class TestStringResource : StringResource {

    private val knownStrings = mapOf(
        // ORCA route strings
        "transit_orca_route_link" to "Link Light Rail",
        "transit_orca_route_sounder" to "Sounder Train",
        "transit_orca_route_express_bus" to "Express Bus",
        "transit_orca_route_bus" to "Bus",
        "transit_orca_route_brt" to "Bus Rapid Transit",
        "transit_orca_route_topup" to "Top-up",
        "transit_orca_route_streetcar" to "Streetcar",
        "transit_orca_route_monorail" to "Seattle Monorail",
        "transit_orca_route_water_taxi" to "Water Taxi",
        // ORCA agency strings (full names)
        "transit_orca_agency_ct" to "Community Transit",
        "transit_orca_agency_et" to "Everett Transit",
        "transit_orca_agency_kcm" to "King County Metro Transit",
        "transit_orca_agency_kt" to "Kitsap Transit",
        "transit_orca_agency_pt" to "Pierce Transit",
        "transit_orca_agency_st" to "Sound Transit",
        "transit_orca_agency_wsf" to "Washington State Ferries",
        "transit_orca_agency_sms" to "Seattle Monorail Services",
        "transit_orca_agency_kcwt" to "King County Water Taxi",
        // ORCA agency strings (short names)
        "transit_orca_agency_ct_short" to "CT",
        "transit_orca_agency_et_short" to "ET",
        "transit_orca_agency_kcm_short" to "KCM",
        "transit_orca_agency_kt_short" to "KT",
        "transit_orca_agency_pt_short" to "PT",
        "transit_orca_agency_st_short" to "ST",
        "transit_orca_agency_wsf_short" to "WSF",
        "transit_orca_agency_sms_short" to "SMS",
        "transit_orca_agency_kcwt_short" to "KCWT",
        "transit_orca_agency_unknown_short" to "Unknown",
        // Opal strings
        "opal_automatic_top_up" to "Automatic top up",
        "opal_agency_tfnsw" to "Transport for NSW",
        "opal_agency_tfnsw_short" to "TfNSW",
    )

    override fun getString(resource: ComposeStringResource): String {
        return knownStrings[resource.key] ?: resource.key
    }

    override fun getString(resource: ComposeStringResource, vararg formatArgs: Any): String {
        val template = knownStrings[resource.key]
        if (template != null) {
            // Simple %s replacement
            var result: String = template
            formatArgs.forEachIndexed { index, arg ->
                result = result.replaceFirst("%s", arg.toString())
                result = result.replace("%${index + 1}\$s", arg.toString())
            }
            return result
        }
        return "${resource.key}: ${formatArgs.joinToString(", ")}"
    }
}
