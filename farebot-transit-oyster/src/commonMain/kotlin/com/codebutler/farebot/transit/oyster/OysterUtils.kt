/*
 * OysterUtils.kt
 *
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

package com.codebutler.farebot.transit.oyster

import com.codebutler.farebot.base.util.getBitsFromBufferLeBits
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

object OysterUtils {
    // Oyster epoch: 1980-01-01 midnight in London
    private val EPOCH: Instant =
        LocalDate(1980, 1, 1).atStartOfDayIn(TimeZone.of("Europe/London"))

    fun parseTimestamp(buf: ByteArray, offset: Int = 0): Instant {
        val day = buf.getBitsFromBufferLeBits(offset, 15)
        val minute = buf.getBitsFromBufferLeBits(offset + 15, 11)
        return EPOCH + day.days + minute.minutes
    }
}
