/*
 * KazanTrip.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.kazan

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

class KazanTrip(
    override val startTimestamp: Instant,
) : Trip() {
    override val mode: Mode
        get() = Mode.OTHER

    override val fare: TransitCurrency?
        get() = null

    companion object {
        private val TZ = TimeZone.of("Europe/Moscow")

        fun parse(raw: ByteArray): KazanTrip? {
            if (raw.byteArrayToInt(1, 3) == 0) return null
            return KazanTrip(parseTime(raw, 1))
        }

        private fun parseTime(
            raw: ByteArray,
            off: Int,
        ): Instant {
            val year = raw[off].toInt() and 0xff
            val month = raw[off + 1].toInt() and 0xff
            val day = raw[off + 2].toInt() and 0xff
            val hour = raw[off + 3].toInt() and 0xff
            val min = raw[off + 4].toInt() and 0xff
            return LocalDateTime(year + 2000, month, day, hour, min)
                .toInstant(TZ)
        }
    }
}
