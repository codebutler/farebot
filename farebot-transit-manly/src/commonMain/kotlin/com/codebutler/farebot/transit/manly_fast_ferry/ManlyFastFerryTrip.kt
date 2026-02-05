/*
 * ManlyFastFerryTrip.kt
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

package com.codebutler.farebot.transit.manly_fast_ferry

import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPurseRecord
import kotlin.time.Instant

/**
 * Trips on the card are "purse debits", and it is not possible to tell it apart from non-ticket
 * usage (like cafe purchases).
 */
class ManlyFastFerryTrip(
    private val purse: ManlyFastFerryPurseRecord,
    private val epoch: Instant
) : Trip() {

    companion object {
        fun create(purse: ManlyFastFerryPurseRecord, epoch: Instant): ManlyFastFerryTrip {
            return ManlyFastFerryTrip(purse, epoch)
        }
    }

    override val startTimestamp: Instant
        get() {
            val offset = purse.day.toLong() * 86400 + purse.minute.toLong() * 60
            return Instant.fromEpochSeconds(epoch.epochSeconds + offset)
        }

    override val fare: TransitCurrency
        get() = TransitCurrency.AUD(purse.transactionValue)

    override val mode: Mode get() = Mode.FERRY
}
