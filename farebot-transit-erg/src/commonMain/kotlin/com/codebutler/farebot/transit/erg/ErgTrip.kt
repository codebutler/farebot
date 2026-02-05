/*
 * ErgTrip.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.erg

import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.erg.record.ErgPurseRecord
import kotlin.time.Instant

/**
 * Represents a transaction (trip or purse debit) on an ERG card.
 *
 * Subclasses can override [currencyFactory] and mode behavior for system-specific formatting.
 */
open class ErgTrip(
    val purse: ErgPurseRecord,
    val epochDate: Int,
    private val currencyFactory: (Int) -> TransitCurrency = { TransitCurrency.XXX(it) }
) : Trip() {

    override val startTimestamp: Instant
        get() = Instant.fromEpochSeconds(convertTimestamp(epochDate, purse.day, purse.minute))

    override val fare: TransitCurrency?
        get() {
            if (purse.transactionValue == 0) return null
            var value = purse.transactionValue
            if (purse.isCredit) {
                value *= -1
            }
            return currencyFactory(value)
        }

    override val mode: Mode get() = Mode.OTHER

    companion object {
        /**
         * Epoch is year 2000 UTC. Day offset + minute offset gives the transaction time.
         */
        private const val EPOCH_2000 = 946684800L // 2000-01-01T00:00:00Z

        fun convertTimestamp(epochDate: Int, day: Int = 0, minute: Int = 0): Long =
            EPOCH_2000 + ((epochDate + day).toLong() * 86400L) + (minute.toLong() * 60L)
    }
}
