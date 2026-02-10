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
        // Japan IC card names
        "card_name_suica" to "Suica",
        "card_name_pasmo" to "PASMO",
        "card_name_icoca" to "ICOCA",
        "card_name_japan_ic" to "Japan IC",
        "card_name_hayakaken" to "Hayakaken",
        "card_name_kitaca" to "Kitaca",
        "card_name_manaca" to "manaca",
        "card_name_nimoca" to "nimoca",
        "card_name_pitapa" to "PiTaPa",
        "card_name_sugoca" to "SUGOCA",
        "card_name_toica" to "TOICA",
        "location_japan" to "Japan",
        // Suica unknown fallback strings
        "suica_unknown_console" to "Console 0x%s",
        "suica_unknown_process" to "Process 0x%s",
        // FeliCa terminal type strings
        "felica_terminal_fare_adjustment" to "Fare Adjustment Machine",
        "felica_terminal_portable" to "Portable Terminal",
        "felica_terminal_vehicle" to "Vehicle Terminal (on bus)",
        "felica_terminal_ticket" to "Ticket Machine",
        "felica_terminal_deposit_quick_charge" to "Quick Charge Machine",
        "felica_terminal_tvm_tokyo_monorail" to "Tokyo Monorail Ticket Machine",
        "felica_terminal_tvm_etc" to "Ticket Machine, etc.",
        "felica_terminal_turnstile" to "Turnstile",
        "felica_terminal_ticket_validator" to "Ticket validator",
        "felica_terminal_ticket_booth" to "Ticket booth",
        "felica_terminal_ticket_office_green" to "Ticket office (Green Window)",
        "felica_terminal_view_altte" to "VIEW ALTTE",
        "felica_terminal_ticket_gate_terminal" to "Ticket Gate Terminal",
        "felica_terminal_mobile_phone" to "Mobile Phone",
        "felica_terminal_connection_adjustment" to "Connection Adjustment Machine",
        "felica_terminal_transfer_adjustment" to "Transfer Adjustment Machine",
        "felica_terminal_simple_deposit" to "Simple Deposit Machine",
        "felica_terminal_pos" to "Point of Sale Terminal",
        "felica_terminal_vending" to "Vending Machine",
        // FeliCa process type strings
        "felica_process_fare_exit_gate" to "Fare Gate",
        "felica_process_charge" to "Charge",
        "felica_process_purchase_magnetic" to "Magnetic Ticket",
        "felica_process_fare_adjustment" to "Fare Adjustment",
        "felica_process_admission_payment" to "Admission Payment",
        "felica_process_booth_exit" to "Station Master Booth Exit",
        "felica_process_issue_new" to "New Issue",
        "felica_process_booth_deduction" to "Booth Deduction",
        "felica_process_bus_pitapa" to "Bus (PiTaPa)",
        "felica_process_bus_iruca" to "Bus (IruCa)",
        "felica_process_reissue" to "Re-issue",
        "felica_process_payment_shinkansen" to "Shinkansen Payment",
        "felica_process_entry_a_autocharge" to "Entry A (Autocharge)",
        "felica_process_exit_a_autocharge" to "Exit A (Autocharge)",
        "felica_process_deposit_bus" to "Bus Deposit",
        "felica_process_purchase_special_ticket" to "Ticket (Special Bus/Streetcar)",
        "felica_process_merchandise_purchase" to "Merchandise",
        "felica_process_bonus_charge" to "Bonus Charge",
        "felica_process_register_deposit" to "Register Deposit",
        "felica_process_merchandise_cancel" to "Cancel Merchandise",
        "felica_process_merchandise_admission" to "Merchandise/Admission",
        "felica_process_merchandise_purchase_cash" to "Merchandise (partially with cash)",
        "felica_process_merchandise_admission_cash" to "Merchandise/Admission (partially with cash)",
        "felica_process_payment_thirdparty" to "Payment (3rd Party)",
        "felica_process_admission_thirdparty" to "Admission Payment (3rd Party)",
        // Clipper strings
        "clipper_agency_actransit" to "AC Transit",
        "clipper_agency_bart" to "BART",
        "clipper_agency_caltrain" to "Caltrain",
        "clipper_agency_ggbhtd" to "Golden Gate",
        "clipper_agency_muni" to "SFMTA (Muni)",
        "clipper_agency_samtrans" to "SamTrans",
        "clipper_agency_vta" to "VTA",
        "clipper_agency_ccta" to "County Connection",
        "clipper_agency_ggf" to "Golden Gate Ferry",
        "clipper_agency_smart" to "SMART",
        "clipper_agency_weta" to "SF Bay Ferry",
        "clipper_agency_unknown" to "Unknown (0x%s)",
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
