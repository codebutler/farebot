/*
 * FelicaUtils.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.card.felica

/**
 * Utilities for working with FeliCa cards.
 */
object FelicaUtils {
    /**
     * Translates the System name to something human readable.
     *
     * Systems in FeliCa are like Applications in MIFARE.  They represent
     * a particular system operator's data.
     *
     * @param systemCode FeliCa system code to translate.
     * @return English string describing the operator of that System.
     */
    fun getFriendlySystemName(systemCode: Int): String =
        when (systemCode) {
            FeliCaConstants.SYSTEMCODE_SUICA -> "Suica"
            FeliCaConstants.SYSTEMCODE_EDY -> "Common / Edy"
            FeliCaConstants.SYSTEMCODE_FELICA_LITE -> "FeliCa Lite"
            FeliCaConstants.SYSTEMCODE_OCTOPUS -> "Octopus"
            else -> "Unknown"
        }

    fun getFriendlyServiceName(
        systemCode: Int,
        serviceCode: Int,
    ): String =
        when (systemCode) {
            FeliCaConstants.SYSTEMCODE_SUICA ->
                when (serviceCode) {
                    FeliCaConstants.SERVICE_SUICA_HISTORY -> "Suica History"
                    FeliCaConstants.SERVICE_SUICA_INOUT -> "Suica In/Out"
                    else -> "Unknown"
                }

            FeliCaConstants.SYSTEMCODE_FELICA_LITE ->
                when (serviceCode) {
                    FeliCaConstants.SERVICE_FELICA_LITE_READONLY -> "FeliCa Lite Read-only"
                    FeliCaConstants.SERVICE_FELICA_LITE_READWRITE -> "Felica Lite Read-write"
                    else -> "Unknown"
                }

            FeliCaConstants.SYSTEMCODE_OCTOPUS ->
                when (serviceCode) {
                    FeliCaConstants.SERVICE_OCTOPUS -> "Octopus Metadata"
                    else -> "Unknown"
                }

            else -> "Unknown"
        }
}
