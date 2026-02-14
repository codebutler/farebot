/*
 * BipUtil.kt
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

package com.codebutler.farebot.transit.bip

import com.codebutler.farebot.base.util.getBitsFromBufferLeBits
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

private val TZ = TimeZone.of("America/Santiago")

internal fun parseTimestamp(raw: ByteArray): Instant? {
    val year = raw.getBitsFromBufferLeBits(15, 5) + 2000
    val month = raw.getBitsFromBufferLeBits(11, 4)
    val day = raw.getBitsFromBufferLeBits(6, 5)
    val hour = raw.getBitsFromBufferLeBits(20, 5)
    val minute = raw.getBitsFromBufferLeBits(25, 6)
    val second = raw.getBitsFromBufferLeBits(31, 6)

    return try {
        LocalDateTime(year, month, day, hour, minute, second)
            .toInstant(TZ)
    } catch (_: Exception) {
        null
    }
}
