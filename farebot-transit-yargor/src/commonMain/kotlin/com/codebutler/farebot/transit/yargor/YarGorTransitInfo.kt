/*
 * YarGorTransitInfo.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.yargor

import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_yargor.generated.resources.Res
import farebot.farebot_transit_yargor.generated.resources.yargor_card_name
import kotlinx.datetime.TimeZone

class YarGorTransitInfo(
    private val mSerial: Long,
    private val mLastTrip: YarGorTrip?,
    private val mSub: YarGorSubscription,
) : TransitInfo() {
    override val serialNumber: String
        get() = formatSerial(mSerial)

    override val cardName: String
        get() = getStringBlocking(Res.string.yargor_card_name)

    override val trips: List<Trip>
        get() = listOfNotNull(mLastTrip)

    override val subscriptions: List<Subscription>
        get() = listOf(mSub)

    companion object {
        val TZ: TimeZone = TimeZone.of("Europe/Moscow")

        fun parse(card: ClassicCard): YarGorTransitInfo =
            YarGorTransitInfo(
                mSub = YarGorSubscription.parse(card.getSector(10) as DataClassicSector),
                mLastTrip = YarGorTrip.parse((card.getSector(12) as DataClassicSector).getBlock(0).data),
                mSerial = getSerial(card),
            )

        fun getSerial(card: ClassicCard): Long = card.tagId.byteArrayToLongReversed()

        fun formatSerial(serial: Long): String {
            val str = (serial + 90000000000L).toString()
            return groupString(str, ".", 4)
        }

        /**
         * Groups a string by inserting a separator every [groupSize] characters.
         */
        private fun groupString(
            value: String,
            separator: String,
            groupSize: Int,
        ): String {
            val ret = StringBuilder()
            var ptr = 0
            while (ptr + groupSize < value.length) {
                ret.append(value, ptr, ptr + groupSize).append(separator)
                ptr += groupSize
            }
            ret.append(value, ptr, value.length)
            return ret.toString()
        }
    }
}
