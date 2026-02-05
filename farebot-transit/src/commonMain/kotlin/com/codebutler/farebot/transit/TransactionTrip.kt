/*
 * TransactionTrip.kt
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

class TransactionTripCapsule(
    var start: Transaction? = null,
    var end: Transaction? = null
)

class TransactionTrip(override val capsule: TransactionTripCapsule) : TransactionTripAbstract() {
    override val fare: TransitCurrency?
        get() {
            // No fare applies to the trip, as the tap-on was reversed.
            if (end?.isCancel == true) {
                return null
            }

            return start?.fare?.let {
                // There is a start fare, add the end fare to it, if present
                it + end?.fare
            } ?: end?.fare // Otherwise use the end fare.
        }

    companion object {
        fun merge(transactionsIn: List<Transaction>): List<TransactionTripAbstract> =
            merge(transactionsIn) { TransactionTrip(makeCapsule(it)) }

        fun merge(vararg transactions: Transaction): List<TransactionTripAbstract> =
            merge(transactions.toList())
    }
}

class TransactionTripLastPrice(override val capsule: TransactionTripCapsule) : TransactionTripAbstract() {
    override val fare: TransitCurrency? get() = end?.fare ?: start?.fare

    companion object {
        fun merge(transactionsIn: List<Transaction>): List<TransactionTripAbstract> =
            merge(transactionsIn) { TransactionTripLastPrice(makeCapsule(it)) }

        fun merge(vararg transactions: Transaction): List<TransactionTripAbstract> =
            merge(transactions.toList())
    }
}

abstract class TransactionTripAbstract : Trip() {
    abstract val capsule: TransactionTripCapsule

    protected val start get() = capsule.start
    protected val end get() = capsule.end

    private val any: Transaction?
        get() = start ?: end

    override val routeName: String?
        get() {
            val startLines = start?.routeNames ?: emptyList()
            val endLines = end?.routeNames ?: emptyList()
            return getRouteName(startLines, endLines)
        }

    override val humanReadableRouteID: String?
        get() {
            val startLines = start?.humanReadableLineIDs ?: emptyList()
            val endLines = end?.humanReadableLineIDs ?: emptyList()
            return getRouteName(startLines, endLines)
        }

    override val passengerCount: Int
        get() = any?.passengerCount ?: -1

    override val vehicleID: String?
        get() = any?.vehicleID

    override val machineID: String?
        get() = any?.machineID

    override val startStation: Station?
        get() = start?.station

    override val endStation: Station?
        get() = end?.station

    override val startTimestamp: Instant?
        get() = start?.timestamp

    override val endTimestamp: Instant?
        get() = end?.timestamp

    override val mode: Mode
        get() = any?.mode ?: Mode.OTHER

    abstract override val fare: TransitCurrency?

    override val isTransfer: Boolean
        get() = any?.isTransfer ?: false

    override val isRejected: Boolean
        get() = any?.isRejected ?: false

    override val agencyName: String?
        get() = any?.agencyName

    override val shortAgencyName: String?
        get() = any?.shortAgencyName

    companion object {
        fun makeCapsule(transaction: Transaction): TransactionTripCapsule =
            if (transaction.isTapOff || transaction.isCancel)
                TransactionTripCapsule(null, transaction)
            else
                TransactionTripCapsule(transaction, null)

        fun merge(
            transactionsIn: List<Transaction>,
            factory: (Transaction) -> TransactionTripAbstract
        ): List<TransactionTripAbstract> {
            val timedTransactions = mutableListOf<Pair<Transaction, Instant>>()
            val unmergeableTransactions = mutableListOf<Transaction>()
            for (transaction in transactionsIn) {
                val ts = transaction.timestamp
                if (!transaction.isTransparent && ts != null)
                    timedTransactions.add(Pair(transaction, ts))
                else
                    unmergeableTransactions.add(transaction)
            }
            val transactions = timedTransactions.sortedBy { it.second }
            val trips = mutableListOf<TransactionTripAbstract>()
            for ((first) in transactions) {
                if (trips.isEmpty()) {
                    trips.add(factory(first))
                    continue
                }
                val previous = trips[trips.size - 1]
                if (previous.end == null && previous.start?.shouldBeMerged(first) == true)
                    previous.capsule.end = first
                else
                    trips.add(factory(first))
            }
            return trips + unmergeableTransactions.map { factory(it) }
        }
    }
}
