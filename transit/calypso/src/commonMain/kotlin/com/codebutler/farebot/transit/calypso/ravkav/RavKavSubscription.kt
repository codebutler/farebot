/*
 * RavKavSubscription.kt
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

package com.codebutler.farebot.transit.calypso.ravkav

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Subscription

internal class RavKavSubscription(
    override val parsed: En1545Parsed,
    override val stringResource: StringResource,
    private val counter: Int?,
) : En1545Subscription() {
    override val lookup: En1545Lookup
        get() = RavKavLookup

    private val ctrUse: Int
        get() {
            val tariffType = parsed.getIntOrZero(En1545Subscription.CONTRACT_TARIFF)
            return (tariffType shr 6) and 0x7
        }

    override val balance: TransitBalance?
        get() {
            if (ctrUse != 3 || counter == null) return null
            return TransitBalance(balance = TransitCurrency(counter, "ILS"))
        }

    override val remainingTripCount: Int?
        get() {
            if (ctrUse == 2 || counter == null) return counter
            return null
        }

    companion object {
        private const val CONTRACT_SALE_NUMBER = "ContractSaleNumber"
        private const val CONTRACT_RESTRICT_DURATION = "ContractRestrictDuration"

        val FIELDS =
            En1545Container(
                En1545FixedInteger("Version", 3),
                En1545FixedInteger.date(En1545Subscription.CONTRACT_START),
                En1545FixedInteger(En1545Subscription.CONTRACT_PROVIDER, 8),
                En1545FixedInteger(En1545Subscription.CONTRACT_TARIFF, 11),
                En1545FixedInteger.date(En1545Subscription.CONTRACT_SALE),
                En1545FixedInteger(En1545Subscription.CONTRACT_SALE_DEVICE, 12),
                En1545FixedInteger(CONTRACT_SALE_NUMBER, 10),
                En1545FixedInteger(En1545Subscription.CONTRACT_INTERCHANGE, 1),
                En1545Bitmap(
                    En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_A, 5),
                    En1545FixedInteger(En1545Subscription.CONTRACT_RESTRICT_CODE, 5),
                    En1545FixedInteger(CONTRACT_RESTRICT_DURATION, 6),
                    En1545FixedInteger.date(En1545Subscription.CONTRACT_END),
                    En1545FixedInteger(En1545Subscription.CONTRACT_DURATION, 8),
                    En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_B, 32),
                    En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_C, 6),
                    En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_D, 32),
                    En1545FixedInteger(En1545Subscription.CONTRACT_UNKNOWN_E, 32),
                ),
            )
    }
}
