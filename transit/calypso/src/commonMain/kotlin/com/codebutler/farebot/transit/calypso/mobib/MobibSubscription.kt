/*
 * MobibSubscription.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.calypso.mobib

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Subscription
import com.codebutler.farebot.transit.en1545.getBitsFromBuffer

internal class MobibSubscription private constructor(
    override val parsed: En1545Parsed,
    override val lookup: En1545Lookup,
    override val stringResource: StringResource,
    private val counter: Int?,
) : En1545Subscription() {
    private val counterUse: Int? = contractTariff?.shr(10)?.and(7)

    override val remainingTripCount: Int? = if (counterUse == 4) null else counter

    companion object {
        private const val CONTRACT_VERSION = "ContractVersion"
        private const val DURATION_UNITS = "DurationUnits"
        private const val NEVER_SEEN_0 = "NeverSeen0"
        private const val NEVER_SEEN_1 = "NeverSeen1"
        private const val NEVER_SEEN_4 = "NeverSeen4"

        fun parse(
            data: ByteArray,
            stringResource: StringResource,
            counter: Int?,
        ): MobibSubscription? {
            if (data.all { it == 0.toByte() }) return null

            val version = data.getBitsFromBuffer(0, 6)
            val fields =
                if (version <= 3) {
                    En1545Container(
                        En1545FixedInteger(CONTRACT_VERSION, 6),
                        En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_B, 35 - 14),
                        En1545FixedInteger(En1545Subscription.CONTRACT_TARIFF, 14),
                        En1545FixedInteger.date(En1545Subscription.CONTRACT_SALE),
                        En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_C, 48),
                        En1545FixedInteger(En1545Subscription.CONTRACT_PRICE_AMOUNT, 16),
                        En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_D, 113),
                    )
                } else {
                    En1545Container(
                        En1545FixedInteger(CONTRACT_VERSION, 6),
                        En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_A, 19),
                        En1545FixedInteger(En1545Subscription.CONTRACT_TARIFF, 14),
                        En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_B, 50),
                        En1545FixedInteger(En1545Subscription.CONTRACT_PRICE_AMOUNT, 16),
                        En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_C, 6),
                        En1545Bitmap(
                            En1545FixedInteger(NEVER_SEEN_0, 5),
                            En1545FixedInteger(NEVER_SEEN_1, 5),
                            En1545FixedInteger.date(En1545Subscription.CONTRACT_SALE),
                            En1545Container(
                                En1545FixedInteger(DURATION_UNITS, 2),
                                En1545FixedInteger(En1545Subscription.CONTRACT_DURATION, 8),
                            ),
                            En1545FixedInteger(NEVER_SEEN_4, 8),
                        ),
                        En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_D, 24),
                    )
                }

            val parsed = En1545Parser.parse(data, fields)
            return MobibSubscription(parsed, MobibLookup, stringResource, counter)
        }
    }
}
