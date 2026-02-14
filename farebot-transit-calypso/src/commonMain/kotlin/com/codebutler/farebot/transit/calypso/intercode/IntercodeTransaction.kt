/*
 * IntercodeTransaction.kt
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

import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.calypso.IntercodeFields
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Transaction

internal class IntercodeTransaction(
    private val networkId: Int,
    override val parsed: En1545Parsed,
) : En1545Transaction() {
    override val lookup: En1545Lookup
        get() = IntercodeTransitInfo.getLookup(networkId)

    override val mode: Trip.Mode
        get() {
            val line = super.routeNumber
            if (networkId == IntercodeTransitInfo.NETWORK_TISSEO && line == 1007) {
                // network Tisseo, line Teleo is wrongly identified as a metro line
                return Trip.Mode.CABLECAR
            }
            return super.mode
        }

    companion object {
        fun parse(
            data: ByteArray,
            networkId: Int,
        ): IntercodeTransaction {
            val parsed = En1545Parser.parse(data, IntercodeFields.TRIP_FIELDS_LOCAL)
            return IntercodeTransaction(
                parsed.getInt(EVENT_NETWORK_ID) ?: networkId,
                parsed,
            )
        }
    }
}
