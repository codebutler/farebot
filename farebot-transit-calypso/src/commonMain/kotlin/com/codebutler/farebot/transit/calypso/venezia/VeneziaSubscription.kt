/*
 * VeneziaSubscription.kt
 *
 * Copyright 2018-2019 Google
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

package com.codebutler.farebot.transit.calypso.venezia

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Subscription
import com.codebutler.farebot.transit.en1545.getBitsFromBuffer

internal class VeneziaSubscription(
    override val parsed: En1545Parsed,
    override val stringResource: StringResource,
    private val counter: Int?
) : En1545Subscription() {

    override val lookup get() = VeneziaLookup

    override val remainingTripCount: Int?
        get() = counter?.div(256)

    companion object {
        private val SUBSCRIPTION_FIELDS = En1545Container(
            En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_A, 6),
            En1545FixedInteger(En1545Subscription.CONTRACT_TARIFF, 16),
            En1545FixedInteger("IdCounter", 8),
            En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_B, 82),
            En1545FixedInteger.datePacked(En1545Subscription.CONTRACT_SALE),
            En1545FixedInteger.timePacked11Local(En1545Subscription.CONTRACT_SALE)
        )

        fun parse(data: ByteArray, stringResource: StringResource, counter: Int?): VeneziaSubscription? {
            if (data.getBitsFromBuffer(0, 22) == 0) {
                return null
            }

            val parsed = En1545Parser.parse(data, SUBSCRIPTION_FIELDS)
            return VeneziaSubscription(parsed, stringResource, counter)
        }
    }
}
