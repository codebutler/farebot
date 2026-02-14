/*
 * ZolotayaKoronaRefill.kt
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

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

class ZolotayaKoronaRefill internal constructor(
    internal val time: Int,
    internal val amount: Int,
    internal val counter: Int,
    private val cardType: Int,
    private val machineIdValue: Int,
) : Trip() {
    override val startTimestamp: Instant?
        get() = ZolotayaKoronaTransitInfo.parseTime(time, cardType)

    override val machineID: String get() = "J$machineIdValue"

    override val fare: TransitCurrency get() = TransitCurrency.RUB(-amount)

    override val mode: Mode get() = Mode.TICKET_MACHINE

    companion object {
        fun parse(
            block: ByteArray,
            cardType: Int,
        ): ZolotayaKoronaRefill? {
            if (block.isAllZero()) return null
            val region = NumberUtils.convertBCDtoInteger(cardType shr 16)
            // known values: 23 -> 1, 76 -> 2
            val guessedHighBits = (region + 28) / 39
            return ZolotayaKoronaRefill(
                machineIdValue = block.byteArrayToIntReversed(1, 2) or (guessedHighBits shl 16),
                time = block.byteArrayToIntReversed(3, 4),
                amount = block.byteArrayToIntReversed(7, 4),
                counter = block.byteArrayToIntReversed(11, 2),
                cardType = cardType,
            )
        }
    }
}
