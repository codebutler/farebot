/*
 * Transaction.kt
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

package com.codebutler.farebot.transit

import kotlin.time.Instant

abstract class Transaction : Comparable<Transaction> {
    abstract val isTapOff: Boolean

    /**
     * Candidate line names associated with the transaction. This is useful if there is a
     * separate field on the card which encodes the route or line taken, and that knowledge of
     * the station alone is not generally sufficient to determine the correct route.
     *
     * By default, this gets candidate route names from the Station.
     */
    open val routeNames: List<String>
        get() = station?.lineNames ?: emptyList()

    /**
     * Candidate human-readable line IDs associated with the transaction.
     *
     * By default, this gets candidate route IDs from the Station.
     */
    open val humanReadableLineIDs: List<String>
        get() = station?.humanReadableLineIds ?: emptyList()

    open val vehicleID: String? get() = null

    open val machineID: String? get() = null

    open val passengerCount: Int get() = -1

    open val station: Station? get() = null

    abstract val timestamp: Instant?

    abstract val fare: TransitCurrency?

    open val mode: Trip.Mode get() = Trip.Mode.OTHER

    open val isCancel: Boolean get() = false

    protected abstract val isTapOn: Boolean

    open val isTransfer: Boolean get() = false

    open val isRejected: Boolean get() = false

    open val isTransparent: Boolean
        get() = mode in listOf(Trip.Mode.TICKET_MACHINE, Trip.Mode.VENDING_MACHINE)

    open val agencyName: String? get() = null

    open val shortAgencyName: String? get() = agencyName

    open fun shouldBeMerged(other: Transaction): Boolean =
        isTapOn && (other.isTapOff || other.isCancel) && isSameTrip(other)

    protected abstract fun isSameTrip(other: Transaction): Boolean

    override fun compareTo(other: Transaction): Int {
        val t1 = timestamp
        val t2 = other.timestamp
        if (t1 == null && t2 == null) return 0
        if (t1 == null) return -1
        if (t2 == null) return +1
        return t1.compareTo(t2)
    }

    class Comparator : kotlin.Comparator<Transaction> {
        override fun compare(
            a: Transaction,
            b: Transaction,
        ): Int = a.compareTo(b)
    }
}
