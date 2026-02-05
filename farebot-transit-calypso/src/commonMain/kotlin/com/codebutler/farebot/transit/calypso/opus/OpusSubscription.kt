/*
 * OpusSubscription.kt
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

package com.codebutler.farebot.transit.calypso.opus

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Subscription
import com.codebutler.farebot.transit.en1545.En1545Subscription.Companion.CONTRACT_END

internal class OpusSubscription(
    override val parsed: En1545Parsed,
    override val stringResource: StringResource,
    private val ctr: Int?
) : En1545Subscription() {

    override val lookup: En1545Lookup
        get() = OpusLookup

    override val remainingTripCount: Int?
        get() = if (parsed.getIntOrZero(En1545FixedInteger.dateName(CONTRACT_END)) == 0)
            ctr else null

    companion object {
        private const val CONTRACT_UNKNOWN_A = "ContractUnknownA"
        private const val CONTRACT_PROVIDER = "ContractProvider"
        private const val CONTRACT_TARIFF = "ContractTariff"
        private const val CONTRACT_START = "ContractStart"
        private const val CONTRACT_END = "ContractEnd"
        private const val CONTRACT_UNKNOWN_B = "ContractUnknownB"
        private const val CONTRACT_SALE = "ContractSale"
        private const val CONTRACT_UNKNOWN_C = "ContractUnknownC"
        private const val CONTRACT_STATUS = "ContractStatus"
        private const val CONTRACT_UNKNOWN_D = "ContractUnknownD"

        val FIELDS = En1545Container(
            En1545FixedInteger(CONTRACT_UNKNOWN_A, 3),
            En1545Bitmap(
                En1545FixedInteger(CONTRACT_PROVIDER, 8),
                En1545FixedInteger(CONTRACT_TARIFF, 16),
                En1545Bitmap(
                    En1545FixedInteger.date(CONTRACT_START),
                    En1545FixedInteger.date(CONTRACT_END)
                ),
                En1545Container(
                    En1545FixedInteger(CONTRACT_UNKNOWN_B, 17),
                    En1545FixedInteger.date(CONTRACT_SALE),
                    En1545FixedInteger.timeLocal(CONTRACT_SALE),
                    En1545FixedHex(CONTRACT_UNKNOWN_C, 36),
                    En1545FixedInteger(CONTRACT_STATUS, 8),
                    En1545FixedHex(CONTRACT_UNKNOWN_D, 36)
                )
            )
        )
    }
}
