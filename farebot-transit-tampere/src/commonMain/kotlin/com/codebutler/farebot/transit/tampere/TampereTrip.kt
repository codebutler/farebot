/*
 * TampereTrip.kt
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

package com.codebutler.farebot.transit.tampere

import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

class TampereTrip(
    private val mDay: Int,
    private val mMinute: Int,
    private val mFare: Int,
    private val mMinutesSinceFirstStamp: Int,
    private val mABC: Int,
    private val mD: Int,
    private val mE: Int,
    private val mF: Int,
    private val mRoute: Int,
    private val mEventCode: Int, // 3 = topup, 5 = first tap, 11 = transfer
    private val mFlags: Int,
    override val passengerCount: Int
) : Trip() {

    override val startTimestamp: Instant
        get() = TampereTransitInfo.parseTimestamp(mDay, mMinute)

    override val fare: TransitCurrency
        get() = TransitCurrency.EUR(
            if (mEventCode == 3) -mFare else mFare
        )

    override val mode: Mode
        get() = when (mEventCode) {
            5, 11 -> if (mRoute / 100 in listOf(1, 3) && mDay >= 0xad7f) Mode.TRAM else Mode.BUS
            3 -> Mode.TICKET_MACHINE
            else -> Mode.OTHER
        }

    override val isTransfer: Boolean
        get() = (mFlags and 0x4) != 0

    override val humanReadableRouteID: String
        get() = "${mRoute / 100}/${mRoute % 100}"

    override val routeName: String?
        get() = getRouteName(mRoute)

    companion object {
        private fun getRouteName(routeNumber: Int): String? =
            when {
                routeNumber == 0 || routeNumber == 1 -> null
                else -> "${routeNumber / 100}"
            }

        fun parse(raw: ByteArray): TampereTrip {
            val minuteField = raw.byteArrayToIntReversed(6, 2)
            val cField = raw.byteArrayToIntReversed(10, 2)
            return TampereTrip(
                mDay = raw.byteArrayToIntReversed(0, 2),
                mMinutesSinceFirstStamp = raw.byteArrayToIntReversed(2, 1),
                mABC = raw.byteArrayToIntReversed(3, 3),
                mMinute = minuteField shr 5,
                mEventCode = minuteField and 0x1f,
                mFare = raw.byteArrayToIntReversed(8, 2),
                mD = cField and 3,
                mRoute = cField shr 2,
                mE = raw.byteArrayToIntReversed(12, 1),
                passengerCount = raw.getBitsFromBuffer(13 * 8, 4),
                mF = raw.getBitsFromBuffer(13 * 8 + 4, 4),
                mFlags = raw.byteArrayToIntReversed(14, 1)
                // Last byte: CRC-8-maxim checksum of the record
            )
        }
    }
}
