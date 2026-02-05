/*
 * EdyUtil.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014, 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.edy

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import com.codebutler.farebot.card.felica.FeliCaUtil

internal object EdyUtil {

    private val EDY_EPOCH: Instant = LocalDateTime(2000, 1, 1, 0, 0, 0).toInstant(TimeZone.of("Asia/Tokyo"))

    fun extractDate(data: ByteArray): Instant? {
        val fulloffset = FeliCaUtil.toInt(data[4], data[5], data[6], data[7])
        if (fulloffset == 0) {
            return null
        }

        val dateoffset = fulloffset ushr 17
        val timeoffset = fulloffset and 0x1ffff

        val offset = dateoffset.toLong() * 86400 + timeoffset.toLong()
        return Instant.fromEpochSeconds(EDY_EPOCH.epochSeconds + offset)
    }
}
