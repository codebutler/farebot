/*
 * CalypsoTransitInfo.kt
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

package com.codebutler.farebot.transit.calypso

import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.CalypsoParseResult

/**
 * Base TransitInfo for Calypso/EN1545 cards, wrapping a [CalypsoParseResult].
 */
abstract class CalypsoTransitInfo(
    protected val result: CalypsoParseResult,
) : TransitInfo() {
    abstract override val cardName: String

    override val balances: List<TransitBalance>?
        get() {
            val b = result.balances
            return if (b.isEmpty()) null else b.map { TransitBalance(balance = it) }
        }

    override val serialNumber: String? get() = result.serial

    override val trips: List<Trip> get() = result.trips

    override val subscriptions: List<Subscription>?
        get() {
            val subs = result.subscriptions
            return if (subs.isEmpty()) null else subs
        }
}
