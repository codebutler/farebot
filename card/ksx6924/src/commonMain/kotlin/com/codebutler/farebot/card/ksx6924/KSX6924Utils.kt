/*
 * KSX6924Utils.kt
 *
 * Copyright 2018 Google
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
package com.codebutler.farebot.card.ksx6924

import com.codebutler.farebot.base.util.NumberUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

object KSX6924Utils {
    const val INVALID_DATETIME = 0xffffffffffffffL
    private const val INVALID_DATE = 0xffffffffL

    /**
     * Parses a BCD-encoded date/time value from a KSX6924 card.
     *
     * Format: YYMMDDHHMMSS (6 bytes, BCD-encoded)
     *
     * @param value The date/time value as a Long
     * @param tz The timezone to use
     * @return An [Instant], or null if the value is invalid
     */
    fun parseHexDateTime(
        value: Long,
        tz: TimeZone,
    ): Instant? {
        if (value == INVALID_DATETIME) {
            return null
        }

        val year = NumberUtils.convertBCDtoInteger((value shr 40).toInt() and 0xFF)
        val month = NumberUtils.convertBCDtoInteger((value shr 32 and 0xffL).toInt())
        val day = NumberUtils.convertBCDtoInteger((value shr 24 and 0xffL).toInt())
        val hour = NumberUtils.convertBCDtoInteger((value shr 16 and 0xffL).toInt())
        val minute = NumberUtils.convertBCDtoInteger((value shr 8 and 0xffL).toInt())
        val second = NumberUtils.convertBCDtoInteger((value and 0xffL).toInt())

        // Handle 2-digit year
        val fullYear = if (year < 80) 2000 + year else 1900 + year

        return try {
            LocalDateTime(
                year = fullYear,
                month = month,
                day = day,
                hour = hour,
                minute = minute,
                second = second,
            ).toInstant(tz)
        } catch (e: Exception) {
            println("[KSX6924] Failed to parse hex datetime: $e")
            null
        }
    }

    /**
     * Parses a BCD-encoded date value from a KSX6924 card.
     *
     * Format: YYYYMMDD (4 bytes, BCD-encoded)
     *
     * @param value The date value as a Long
     * @return A [LocalDate], or null if the value is invalid
     */
    fun parseHexDate(value: Long): LocalDate? {
        if (value >= INVALID_DATE) {
            return null
        }

        val year = NumberUtils.convertBCDtoInteger((value shr 16).toInt() and 0xFFFF)
        val month = NumberUtils.convertBCDtoInteger((value shr 8 and 0xffL).toInt())
        val day = NumberUtils.convertBCDtoInteger((value and 0xffL).toInt())

        return try {
            LocalDate(year = year, month = month, day = day)
        } catch (e: Exception) {
            println("[KSX6924] Failed to parse hex date: $e")
            null
        }
    }

    /**
     * Converts a [LocalDate] to an [Instant] at the start of the day in the given timezone.
     */
    fun localDateToInstant(
        date: LocalDate,
        tz: TimeZone,
    ): Instant = LocalDateTime(date, kotlinx.datetime.LocalTime(0, 0)).toInstant(tz)
}
