/*
 * GautrainSubscription.kt
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

package com.codebutler.farebot.transit.gautrain

import com.codebutler.farebot.base.util.DefaultStringResource
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Subscription

private fun neverSeenField(i: Int) = En1545FixedInteger("NeverSeen$i", 8)

/**
 * EN1545 subscription fields for OVChip-format subscriptions (reversed bitmap).
 * Matches Metrodroid's OVChipSubscription.fields(reversed = true).
 */
internal val GAUTRAIN_SUB_FIELDS = En1545Container(
    En1545Bitmap(
        neverSeenField(1),
        En1545FixedInteger(En1545Subscription.CONTRACT_PROVIDER, 16),
        En1545FixedInteger(En1545Subscription.CONTRACT_TARIFF, 16),
        En1545FixedInteger(En1545Subscription.CONTRACT_SERIAL_NUMBER, 32),
        neverSeenField(5),
        En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_A, 10),
        neverSeenField(7),
        neverSeenField(8),
        neverSeenField(9),
        neverSeenField(10),
        neverSeenField(11),
        neverSeenField(12),
        neverSeenField(13),
        En1545Bitmap(
            En1545FixedInteger.date(En1545Subscription.CONTRACT_START),
            En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_START),
            En1545FixedInteger.date(En1545Subscription.CONTRACT_END),
            En1545FixedInteger.timeLocal(En1545Subscription.CONTRACT_END),
            En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_C, 53),
            En1545FixedInteger("NeverSeenB", 8),
            En1545FixedInteger("NeverSeenC", 8),
            En1545FixedInteger("NeverSeenD", 8),
            En1545FixedInteger("NeverSeenE", 8),
            reversed = true
        ),
        En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_D, 40),
        En1545FixedInteger(En1545Subscription.CONTRACT_SALE_DEVICE, 24),
        neverSeenField(16),
        neverSeenField(17),
        neverSeenField(18),
        neverSeenField(19),
        reversed = true
    )
)

class GautrainSubscription(
    override val parsed: En1545Parsed,
    override val stringResource: StringResource,
    private val mType1: Int,
    private val mUsed: Int
) : En1545Subscription() {

    override val subscriptionState: SubscriptionState
        get() = if (mType1 != 0) {
            if (mUsed != 0) SubscriptionState.USED else SubscriptionState.STARTED
        } else SubscriptionState.INACTIVE

    override val lookup: En1545Lookup = GautrainLookup

    companion object {
        fun parse(data: ByteArray, stringResource: StringResource, type1: Int, used: Int): GautrainSubscription =
            GautrainSubscription(
                parsed = En1545Parser.parse(data, GAUTRAIN_SUB_FIELDS),
                stringResource = stringResource,
                mType1 = type1,
                mUsed = used
            )
    }
}
