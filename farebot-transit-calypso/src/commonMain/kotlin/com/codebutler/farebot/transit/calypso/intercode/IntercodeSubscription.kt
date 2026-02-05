/*
 * IntercodeSubscription.kt
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

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.calypso.IntercodeFields
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Subscription

internal class IntercodeSubscription(
    override val parsed: En1545Parsed,
    override val stringResource: StringResource,
    private val ctr: Int?,
    private val networkId: Int
) : En1545Subscription() {

    override val lookup: En1545Lookup
        get() = IntercodeTransitInfo.getLookup(networkId)

    override val remainingTripCount: Int?
        get() {
            if (parsed.getIntOrZero(CONTRACT_DEBIT_SOLD) != 0 && parsed.getIntOrZero(CONTRACT_SOLD) != 0) {
                return ctr!! / parsed.getIntOrZero(CONTRACT_DEBIT_SOLD)
            }
            if (networkId == IntercodeTransitInfo.NETWORK_NAVIGO && parsed.getIntOrZero(CONTRACT_JOURNEYS) != 0 && ctr != null)
                return ctr shr 18
            return if (parsed.getIntOrZero(CONTRACT_JOURNEYS) != 0) {
                ctr
            } else null
        }

    override val totalTripCount: Int?
        get() {
            if (parsed.getIntOrZero(CONTRACT_DEBIT_SOLD) != 0 && parsed.getIntOrZero(CONTRACT_SOLD) != 0) {
                return parsed.getIntOrZero(CONTRACT_SOLD) / parsed.getIntOrZero(CONTRACT_DEBIT_SOLD)
            }
            if (networkId == IntercodeTransitInfo.NETWORK_NAVIGO && parsed.getIntOrZero(CONTRACT_JOURNEYS) != 0)
                return parsed.getIntOrZero(CONTRACT_JOURNEYS) and 0xfff
            return if (parsed.getIntOrZero(CONTRACT_JOURNEYS) != 0) {
                parsed.getIntOrZero(CONTRACT_JOURNEYS)
            } else null
        }

    companion object {
        fun parse(data: ByteArray, type: Int, networkId: Int, ctr: Int?, stringResource: StringResource): IntercodeSubscription {
            val parsed = En1545Parser.parse(data, IntercodeFields.getSubscriptionFields(type))
            val nid = parsed.getInt(CONTRACT_NETWORK_ID)
            return IntercodeSubscription(parsed = parsed, stringResource = stringResource, ctr = ctr, networkId = nid ?: networkId)
        }
    }
}
