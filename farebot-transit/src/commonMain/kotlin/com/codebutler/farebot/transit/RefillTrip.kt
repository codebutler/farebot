/*
 * RefillTrip.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit

import com.codebutler.farebot.base.util.StringResource
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Wrapper around Refills to make them like Trips, so Trips become like history. This is similar
 * to what the Japanese cards (Edy, Suica) already had implemented for themselves.
 */
@Serializable
data class RefillTrip(
    @Contextual val refill: Refill,
    private val stringResource: StringResource,
) : Trip() {
    override val startTimestamp: Instant?
        get() {
            val ts = refill.getTimestamp()
            return if (ts > 0) Instant.fromEpochSeconds(ts) else null
        }

    override val agencyName: String? get() = refill.getAgencyName(stringResource)

    override val shortAgencyName: String? get() = refill.getShortAgencyName(stringResource)

    override val fare: TransitCurrency?
        get() {
            val amountStr = refill.getAmountString(stringResource)
            // RefillTrip delegates fare formatting to the Refill, which returns a pre-formatted string.
            // Until Refill is refactored to return TransitCurrency, we return null here.
            // The amount is displayed via the TransitInfo's refill list instead.
            return null
        }

    override val mode: Mode get() = Mode.TICKET_MACHINE

    companion object {
        fun create(
            refill: Refill,
            stringResource: StringResource,
        ): RefillTrip = RefillTrip(refill, stringResource)
    }
}
