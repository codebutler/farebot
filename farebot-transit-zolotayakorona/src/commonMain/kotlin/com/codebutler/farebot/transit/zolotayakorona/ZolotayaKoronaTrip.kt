/*
 * ZolotayaKoronaTrip.kt
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

package com.codebutler.farebot.transit.zolotayakorona

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

private const val DEFAULT_FARE = 1300

class ZolotayaKoronaTrip internal constructor(
    private val validator: String,
    internal val time: Int,
    private val cardType: Int,
    private val trackNumber: Int,
    private val previousBalance: Int,
    private val nextBalance: Int?,
    private val fieldA: Int,
    private val fieldB: Int,
    private val fieldC: Int,
) : Trip() {
    private val estimatedFare: Int?
        get() =
            when (cardType) {
                0x760500 -> 1150
                0x230100 -> 1275
                else -> null
            }

    internal val estimatedBalance: Int
        get() = previousBalance - (estimatedFare ?: DEFAULT_FARE)

    override val startTimestamp: Instant?
        get() = ZolotayaKoronaTransitInfo.parseTime(time, cardType)

    override val machineID: String get() = "J$validator"

    override val fare: TransitCurrency?
        get() {
            if (nextBalance != null) {
                // Happens if one trip is followed by more than one refill
                if (previousBalance - nextBalance < -500) return null
                return TransitCurrency.RUB(previousBalance - nextBalance)
            }
            return TransitCurrency.RUB(estimatedFare ?: return null)
        }

    override val mode: Mode get() = Mode.BUS

    companion object {
        fun parse(
            block: ByteArray,
            cardType: Int,
            refill: ZolotayaKoronaRefill?,
            balance: Int?,
        ): ZolotayaKoronaTrip? {
            if (block.isAllZero()) return null
            val time = block.byteArrayToIntReversed(6, 4)
            var balanceAfter: Int? = null
            if (balance != null) {
                balanceAfter = balance
                if (refill != null && refill.time > time) {
                    balanceAfter -= refill.amount
                }
            }
            return ZolotayaKoronaTrip(
                fieldA = block.byteArrayToInt(0, 2),
                validator = block.getHexString(2, 3),
                fieldB = block[5].toInt() and 0xff,
                time = time,
                trackNumber = block.byteArrayToInt(10, 1),
                previousBalance = block.byteArrayToIntReversed(11, 4),
                fieldC = block[15].toInt() and 0xff,
                nextBalance = balanceAfter,
                cardType = cardType,
            )
        }
    }
}
