/*
 * YarGorSubscription.kt
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

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.Subscription
import farebot.farebot_transit_yargor.generated.resources.Res
import farebot.farebot_transit_yargor.generated.resources.yargor_mode_bus
import farebot.farebot_transit_yargor.generated.resources.yargor_mode_tram
import farebot.farebot_transit_yargor.generated.resources.yargor_mode_trolleybus
import farebot.farebot_transit_yargor.generated.resources.yargor_sub_allday_all
import farebot.farebot_transit_yargor.generated.resources.yargor_sub_weekday_tram
import farebot.farebot_transit_yargor.generated.resources.yargor_sub_weekday_trolley
import farebot.farebot_transit_yargor.generated.resources.yargor_unknown_format
import com.codebutler.farebot.base.util.getStringBlocking
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant

class YarGorSubscription(
    override val validFrom: Instant,
    override val validTo: Instant,
    override val purchaseTimestamp: Instant,
    private val mType: Int,
    private val mTransports: Byte,
) : Subscription() {

    override val subscriptionName: String?
        get() = when (mType) {
            0x9613 -> getStringBlocking(Res.string.yargor_sub_weekday_tram)
            0x9615 -> getStringBlocking(Res.string.yargor_sub_weekday_trolley)
            0x9621 -> getStringBlocking(Res.string.yargor_sub_allday_all)
            else -> getStringBlocking(Res.string.yargor_unknown_format, mType.toString(16))
        }

    private val transportsDesc: String
        get() {
            val t = mutableListOf<String>()
            for (i in 0..7) {
                if ((mTransports.toInt() and (0x1 shl i)) != 0)
                    t += when (i) {
                        0 -> getStringBlocking(Res.string.yargor_mode_bus)
                        1 -> getStringBlocking(Res.string.yargor_mode_tram)
                        2 -> getStringBlocking(Res.string.yargor_mode_trolleybus)
                        else -> getStringBlocking(Res.string.yargor_unknown_format, i.toString())
                    }
            }
            return t.joinToString()
        }

    companion object {
        private fun parseDate(data: ByteArray, off: Int): Instant {
            val year = 2000 + (data[off].toInt() and 0xff)
            val month = (data[off + 1].toInt() and 0xff)
            val day = (data[off + 2].toInt() and 0xff)
            // Use noon to represent a date-only value
            val ldt = LocalDateTime(year, month, day, 12, 0, 0)
            return ldt.toInstant(YarGorTransitInfo.TZ)
        }

        private fun parseTimestamp(data: ByteArray, off: Int): Instant {
            val year = 2000 + (data[off].toInt() and 0xff)
            val month = (data[off + 1].toInt() and 0xff)
            val day = data[off + 2].toInt() and 0xff
            val hour = data[off + 3].toInt() and 0xff
            val min = data[off + 4].toInt() and 0xff
            val ldt = LocalDateTime(year, month, day, hour, min, 0)
            return ldt.toInstant(YarGorTransitInfo.TZ)
        }

        fun parse(sector: DataClassicSector): YarGorSubscription {
            val block0 = sector.getBlock(0).data
            val block1 = sector.getBlock(1).data
            return YarGorSubscription(
                mType = block0.byteArrayToInt(0, 2),
                validFrom = parseDate(block0, 2),
                validTo = parseDate(block0, 5),
                mTransports = block0[14],
                purchaseTimestamp = parseTimestamp(block1, 0),
            )
        }
    }
}
