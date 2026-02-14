/*
 * OVChipSubscription.kt
 *
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2012 Eric Butler <eric@codebutler.com>
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

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Subscription

class OVChipSubscription internal constructor(
    override val parsed: En1545Parsed,
    override val stringResource: StringResource,
    private val mType1: Int,
    private val mUsed: Int,
) : En1545Subscription() {
    override val subscriptionState get(): Subscription.SubscriptionState =
        if (mType1 != 0) {
            if (mUsed != 0) Subscription.SubscriptionState.USED else Subscription.SubscriptionState.STARTED
        } else {
            Subscription.SubscriptionState.INACTIVE
        }

    override val lookup: En1545Lookup get() = OvcLookup

    companion object {
        private fun neverSeen(i: Int) = "NeverSeen$i"

        // Sizes fully invented
        private fun neverSeenField(i: Int) = En1545FixedInteger(neverSeen(i), 8)

        fun fields(reversed: Boolean = false) =
            En1545Container(
                En1545Bitmap(
                    neverSeenField(1),
                    En1545FixedInteger(CONTRACT_PROVIDER, 16),
                    En1545FixedInteger(CONTRACT_TARIFF, 16),
                    En1545FixedInteger(CONTRACT_SERIAL_NUMBER, 32),
                    neverSeenField(5),
                    En1545FixedInteger(CONTRACT_UNKNOWN_A, 10),
                    neverSeenField(7),
                    neverSeenField(8),
                    neverSeenField(9),
                    neverSeenField(10),
                    neverSeenField(11),
                    neverSeenField(12),
                    neverSeenField(13),
                    En1545Bitmap(
                        En1545FixedInteger.date(CONTRACT_START),
                        En1545FixedInteger.timeLocal(CONTRACT_START),
                        En1545FixedInteger.date(CONTRACT_END),
                        En1545FixedInteger.timeLocal(CONTRACT_END),
                        En1545FixedHex(CONTRACT_UNKNOWN_C, 53),
                        En1545FixedInteger("NeverSeenB", 8),
                        En1545FixedInteger("NeverSeenC", 8),
                        En1545FixedInteger("NeverSeenD", 8),
                        En1545FixedInteger("NeverSeenE", 8),
                        reversed = reversed,
                    ),
                    En1545FixedHex(CONTRACT_UNKNOWN_D, 40),
                    En1545FixedInteger(CONTRACT_SALE_DEVICE, 24),
                    neverSeenField(16),
                    neverSeenField(17),
                    neverSeenField(18),
                    neverSeenField(19),
                    reversed = reversed,
                ),
            )

        fun parse(
            data: ByteArray,
            type1: Int,
            used: Int,
            stringResource: StringResource,
        ): OVChipSubscription =
            OVChipSubscription(
                parsed = En1545Parser.parse(data, fields()),
                stringResource = stringResource,
                mType1 = type1,
                mUsed = used,
            )
    }
}
