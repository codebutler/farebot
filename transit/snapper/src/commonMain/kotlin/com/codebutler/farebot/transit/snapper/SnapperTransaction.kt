/*
 * SnapperTransaction.kt
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
 *
 * Reference: https://github.com/micolous/metrodroid/wiki/Snapper
 */

package com.codebutler.farebot.transit.snapper

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.card.ksx6924.KSX6924Utils.parseHexDateTime
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

class SnapperTransaction(
    val journeyId: Int,
    val seq: Int,
    override val isTapOn: Boolean,
    val type: Int,
    val cost: Int,
    val time: Long,
    val operator: String,
) : Transaction() {
    override val isTapOff get() = !isTapOn

    override val station get() = Station.nameOnly("$journeyId / $seq")

    override val mode get() =
        when (type) {
            2 -> Trip.Mode.BUS
            else -> Trip.Mode.TROLLEYBUS
        }

    override fun isSameTrip(other: Transaction): Boolean {
        val o = other as SnapperTransaction
        return journeyId == o.journeyId && seq == o.seq
    }

    override val timestamp: Instant? get() = parseHexDateTime(time, TZ)

    override val fare get() = TransitCurrency.NZD(cost)

    override val isTransfer get() = seq != 0

    companion object {
        private val TZ = TimeZone.of("Pacific/Auckland")

        fun parseTransaction(
            trip: ByteArray,
            balance: ByteArray,
        ): SnapperTransaction {
            val journeyId = trip[5].toInt()
            val seq = trip[4].toInt()

            val time = trip.byteArrayToLong(13, 7)

            val tapOn = (trip[51].toInt() and 0x10) == 0x10

            val type = balance[0].toInt()
            var cost = balance.byteArrayToInt(10, 4)
            if (type == 2) {
                cost = -cost
            }

            val operator = balance.getHexString(14, 5).substring(0, 9)

            return SnapperTransaction(journeyId, seq, tapOn, type, cost, time, operator)
        }
    }
}
