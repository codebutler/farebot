/*
 * TouchnGoUtil.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
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

package com.codebutler.farebot.transit.touchngo

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.card.classic.DataClassicSector
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

internal const val TNG_STR = "touchngo"

private val TZ_KUALA_LUMPUR = TimeZone.of("Asia/Kuala_Lumpur")

/**
 * Parses a full timestamp (date + time) from Touch 'n Go card data.
 *
 * Bit layout starting at byte offset [off]:
 * - 5 bits: hour
 * - 6 bits: minute
 * - 6 bits: second
 * - 6 bits: year (offset from 1990)
 * - 4 bits: month (1-based)
 * - 5 bits: day
 */
internal fun parseTimestamp(input: ByteArray, off: Int): Instant {
    val hour = input.getBitsFromBuffer(off * 8, 5)
    val min = input.getBitsFromBuffer(off * 8 + 5, 6)
    val sec = input.getBitsFromBuffer(off * 8 + 11, 6)
    val year = input.getBitsFromBuffer(off * 8 + 17, 6) + 1990
    val month = input.getBitsFromBuffer(off * 8 + 23, 4)
    val day = input.getBitsFromBuffer(off * 8 + 27, 5)
    val ldt = LocalDateTime(year, month, day, hour, min, sec)
    return ldt.toInstant(TZ_KUALA_LUMPUR)
}

/**
 * Parses a date-only stamp from Touch 'n Go card data.
 *
 * Bit layout starting at byte offset [off]:
 * - 1 bit: unused
 * - 6 bits: year (offset from 1990)
 * - 4 bits: month (1-based)
 * - 5 bits: day
 */
internal fun parseDaystamp(input: ByteArray, off: Int): LocalDate {
    val y = input.getBitsFromBuffer(off * 8 + 1, 6) + 1990
    val month = input.getBitsFromBuffer(off * 8 + 7, 4)
    val d = input.getBitsFromBuffer(off * 8 + 11, 5)
    return LocalDate(y, month, d)
}

/**
 * Checks if a transit trip is currently in progress (tap-on without tap-off).
 */
internal fun isTripInProgress(sector: DataClassicSector): Boolean {
    return sector.getBlock(1).data.byteArrayToInt(2, 4) != 0
}
