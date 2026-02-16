/*
 * IntercodeTransitInfo.kt
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

package com.codebutler.farebot.transit.calypso.intercode

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.calypso.CalypsoTransitInfo
import com.codebutler.farebot.transit.calypso.IntercodeFields
import com.codebutler.farebot.transit.en1545.CalypsoParseResult
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545TransitData

internal class IntercodeTransitInfo(
    result: CalypsoParseResult,
) : CalypsoTransitInfo(result) {
    override val cardName: FormattedString
        get() {
            val networkId = result.ticketEnv.getIntOrZero(En1545TransitData.ENV_NETWORK_ID)
            return getLookup(networkId).cardName { result.ticketEnv }
                ?: fallbackCardName(networkId)
        }

    companion object {
        private const val COUNTRY_ID_FRANCE = 0x250
        const val NETWORK_NAVIGO = 0x250901
        const val NETWORK_TISSEO = 0x250916

        // NOTE: Many French smart-cards don't have a brand name, and are simply referred to as a
        // "titre de transport" (ticket). Here they take the name of the transit agency.

        private val NETWORKS: Map<Int, IntercodeLookup> =
            mapOf(
                0x250000 to IntercodeLookupPassPass,
                0x250064 to IntercodeLookupUnknown("TaM"),
                0x250502 to IntercodeLookupOura,
                NETWORK_NAVIGO to IntercodeLookupNavigo,
                0x250908 to IntercodeLookupUnknown("KorriGo"),
                NETWORK_TISSEO to IntercodeLookupTisseo,
                0x250920 to IntercodeLookupUnknown("Envibus"),
                0x250921 to IntercodeLookupGironde,
            )

        fun getLookup(networkId: Int): IntercodeLookup = NETWORKS[networkId] ?: IntercodeLookupUnknown(null)

        fun isIntercode(networkId: Int): Boolean = NETWORKS[networkId] != null || COUNTRY_ID_FRANCE == networkId shr 12

        internal fun fallbackCardName(networkId: Int): FormattedString =
            if (networkId shr 12 == COUNTRY_ID_FRANCE) {
                FormattedString("Intercode-France-" + (networkId and 0xfff).toString(16))
            } else {
                FormattedString("Intercode-" + networkId.toString(16))
            }

        internal fun getCardName(
            networkId: Int,
            env: ByteArray,
        ): FormattedString =
            getLookup(networkId).cardName { parseTicketEnv(env) }
                ?: fallbackCardName(networkId)

        internal fun parseTicketEnv(tenv: ByteArray) =
            En1545Parser.parse(tenv, IntercodeFields.TICKET_ENV_HOLDER_FIELDS)

        val allCardNames: List<FormattedString>
            get() = NETWORKS.values.flatMap { it.allCardNames }
    }
}
