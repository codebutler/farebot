/*
 * EasyCardTransaction.kt
 *
 * Copyright 2017 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * Based on code from:
 * - http://www.fuzzysecurity.com/tutorials/rfid/4.html
 * - Farebot <https://codebutler.github.io/farebot/>
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

package com.codebutler.farebot.transit.easycard

import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

/**
 * Represents a single transaction (tap on/off) on an EasyCard.
 */
data class EasyCardTransaction(
    val timestampRaw: Long,
    private val rawFare: Int,
    val location: Int,
    private val isEndTap: Boolean,
    private val machineIdRaw: Long,
) : Transaction() {
    constructor(data: ByteArray) : this(
        timestampRaw = data.byteArrayToLongReversed(1, 4),
        rawFare = data.byteArrayToIntReversed(6, 2),
        location = data[11].toInt() and 0xFF,
        isEndTap = data[5] == 0x11.toByte(),
        machineIdRaw = data.byteArrayToLongReversed(12, 4),
    )

    override val fare: TransitCurrency get() = TransitCurrency.TWD(rawFare)

    override val timestamp: Instant? get() = EasyCardTransitFactory.parseTimestamp(timestampRaw)

    override val station: Station?
        get() =
            when (location) {
                BUS -> null
                POS -> null
                else -> EasyCardTransitFactory.lookupStation(location)
            }

    override val mode: Trip.Mode
        get() =
            when (location) {
                BUS -> Trip.Mode.BUS
                POS -> Trip.Mode.POS
                else -> Trip.Mode.METRO
            }

    override val machineID: String get() = "0x${machineIdRaw.toString(16)}"

    override fun isSameTrip(other: Transaction): Boolean {
        if (other !is EasyCardTransaction) {
            return false
        }

        // Bus and POS transactions don't merge
        if (location == POS ||
            location == BUS ||
            other.location == POS ||
            other.location == BUS
        ) {
            return false
        }

        // Merge if this is tap-on and other is tap-off
        return (!isEndTap && other.isEndTap)
    }

    override val isTapOff: Boolean get() = isEndTap

    override val isTapOn: Boolean get() = !isEndTap

    override val routeNames: List<String>
        get() =
            when (mode) {
                Trip.Mode.METRO -> super.routeNames
                else -> emptyList()
            }

    companion object {
        internal const val POS = 1
        internal const val BUS = 5

        /**
         * Parse all trips from a Classic card.
         * Trips are stored in sectors 3-5, excluding trailer blocks.
         */
        internal fun parseTrips(card: ClassicCard): List<Trip> {
            val blocks = mutableListOf<ByteArray>()

            // Sector 3: blocks 1-2 (block 0 and 3 are special)
            (card.getSector(3) as? DataClassicSector)?.let { sector ->
                blocks.addAll(sector.blocks.subList(1, 3).map { it.data })
            }

            // Sector 4: blocks 0-2
            (card.getSector(4) as? DataClassicSector)?.let { sector ->
                blocks.addAll(sector.blocks.subList(0, 3).map { it.data })
            }

            // Sector 5: blocks 0-2
            (card.getSector(5) as? DataClassicSector)?.let { sector ->
                blocks.addAll(sector.blocks.subList(0, 3).map { it.data })
            }

            // Filter out empty blocks and parse transactions
            val transactions =
                blocks
                    .filter { !it.all { b -> b == 0.toByte() } }
                    .map { EasyCardTransaction(it) }
                    .distinctBy { it.timestamp }

            // Merge tap-on/tap-off into trips
            return TransactionTrip.merge(transactions)
        }
    }
}
