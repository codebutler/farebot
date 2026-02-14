/*
 * TMoneyTrip.kt
 *
 * Copyright 2018 Google
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.tmoney

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.card.ksx6924.KSX6924Utils
import com.codebutler.farebot.card.ksx6924.KSX6924Utils.INVALID_DATETIME
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Represents a trip or transaction on a T-Money card.
 *
 * T-Money transaction records are 46 bytes long and contain:
 * - 1 byte: transaction type
 * - 1 byte: unknown
 * - 4 bytes: balance after transaction
 * - 4 bytes: counter
 * - 4 bytes: cost
 * - 2 bytes: unknown
 * - 1 byte: type?
 * - 7 bytes: unknown
 * - 7 bytes: timestamp (BCD-encoded)
 * - 7 bytes: zero
 * - 4 bytes: unknown
 * - 2 bytes: zero
 */
@Serializable
data class TMoneyTrip(
    private val type: Int,
    private val cost: Int,
    private val time: Long,
    private val balanceAfter: Int,
) : Trip() {
    override val fare: TransitCurrency
        get() = TransitCurrency.KRW(cost)

    override val mode: Mode
        get() =
            when (type) {
                TYPE_TOP_UP -> Mode.TICKET_MACHINE
                else -> Mode.OTHER
            }

    override val startTimestamp: Instant?
        get() = KSX6924Utils.parseHexDateTime(time, TZ)

    companion object {
        private val TZ = TimeZone.of("Asia/Seoul")

        private const val TYPE_TOP_UP = 2

        /**
         * Parses a T-Money transaction record.
         *
         * @param data The raw 46-byte transaction record
         * @return A [TMoneyTrip] if the record contains valid data, or null otherwise
         */
        fun parseTrip(data: ByteArray): TMoneyTrip? {
            // 1 byte type
            val type = data[0].toInt() and 0xFF
            // 1 byte unknown
            // 4 bytes balance after transaction
            val balance = data.byteArrayToInt(2, 4)
            // 4 bytes counter
            // 4 bytes cost
            var cost = data.byteArrayToInt(10, 4)
            if (type == TYPE_TOP_UP) {
                cost = -cost
            }
            // 2 bytes unknown
            // 1 byte type??
            // 7 bytes unknown
            // 7 bytes time
            val time = data.byteArrayToLong(26, 7)
            // 7 bytes zero
            // 4 bytes unknown
            // 2 bytes zero

            return if (cost == 0 && time == INVALID_DATETIME) null else TMoneyTrip(type, cost, time, balance)
        }
    }
}
