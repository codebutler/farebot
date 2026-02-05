/*
 * KomuterLinkTrip.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.komuterlink

import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class KomuterLinkTrip(
    private val mAmount: Int,
    private val mNewBalance: Int,
    private val mStartTimestamp: Instant,
    override val mode: Mode,
    private val mTransactionId: Int
) : Trip() {

    override val startTimestamp: Instant
        get() = mStartTimestamp

    override val fare: TransitCurrency
        get() = TransitCurrency.MYR(mAmount)

    companion object {
        private val TZ = TimeZone.of("Asia/Kuala_Lumpur")

        private fun parseTimestamp(data: ByteArray, off: Int): Instant {
            val hour = data.getBitsFromBuffer(off * 8, 5)
            val min = data.getBitsFromBuffer(off * 8 + 5, 6)
            val y = data.getBitsFromBuffer(off * 8 + 17, 6) + 2000
            val month = data.getBitsFromBuffer(off * 8 + 23, 4)
            val d = data.getBitsFromBuffer(off * 8 + 27, 5)
            val ldt = LocalDateTime(y, month, d, hour, min)
            return ldt.toInstant(TZ)
        }

        fun parse(sector: DataClassicSector, sign: Int, mode: Mode): KomuterLinkTrip? {
            val block0 = sector.getBlock(0)
            if (block0.isEmpty) return null
            val data = block0.data
            return KomuterLinkTrip(
                mAmount = data.byteArrayToInt(10, 2) * sign,
                mNewBalance = data.byteArrayToInt(14, 2),
                mStartTimestamp = parseTimestamp(data, 0),
                mode = mode,
                mTransactionId = data.byteArrayToInt(4, 2)
            )
        }
    }
}
