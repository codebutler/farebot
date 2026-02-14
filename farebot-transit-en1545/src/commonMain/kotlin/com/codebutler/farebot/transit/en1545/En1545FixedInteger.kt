/*
 * En1545FixedInteger.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.en1545

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class En1545FixedInteger(
    private val name: String,
    private val len: Int,
) : En1545Field {
    override fun parseField(
        b: ByteArray,
        off: Int,
        path: String,
        holder: En1545Parsed,
        bitParser: En1545Bits,
    ): Int {
        try {
            holder.insertInt(name, path, bitParser(b, off, len))
        } catch (_: Exception) {
        }
        return off + len
    }

    companion object {
        private val EPOCH = LocalDate(1997, 1, 1)

        fun dateName(base: String) = "${base}Date"

        fun datePackedName(base: String) = "${base}DatePacked"

        fun dateBCDName(base: String) = "${base}DateBCD"

        fun timeName(base: String) = "${base}Time"

        fun timePacked16Name(base: String) = "${base}TimePacked16"

        fun timePacked11LocalName(base: String) = "${base}TimePacked11Local"

        fun timeLocalName(base: String) = "${base}TimeLocal"

        fun dateTimeName(base: String) = "${base}DateTime"

        fun dateTimeLocalName(base: String) = "${base}DateTimeLocal"

        private fun utcEpoch(): Instant = EPOCH.atStartOfDayIn(TimeZone.UTC)

        private fun localEpoch(tz: TimeZone): Instant = EPOCH.atStartOfDayIn(tz)

        fun parseTime(
            d: Int,
            t: Int,
            tz: TimeZone,
        ): Instant? {
            if (d == 0 && t == 0) return null
            return utcEpoch() + d.days + t.minutes
        }

        fun parseTimeLocal(
            d: Int,
            t: Int,
            tz: TimeZone,
        ): Instant? {
            if (d == 0 && t == 0) return null
            return localEpoch(tz) + d.days + t.minutes
        }

        fun parseTimePacked16(
            d: Int,
            t: Int,
            tz: TimeZone,
        ): Instant? {
            if (d == 0 && t == 0) return null
            val hours = t shr 11
            val minutes = (t shr 5) and 0x3f
            val secs = (t and 0x1f) * 2
            return utcEpoch() + d.days + hours.toLong().let { it * 3600 }.seconds +
                minutes.toLong().let { it * 60 }.seconds + secs.seconds
        }

        fun parseTimePacked11Local(
            day: Int,
            time: Int,
            tz: TimeZone,
        ): Instant? {
            if (day == 0) return null
            val year = (day shr 9) + 2000
            val month = (day shr 5) and 0xf
            val dayOfMonth = day and 0x1f
            val hour = time shr 6
            val minute = time and 0x3f
            return LocalDateTime(year, month, dayOfMonth, hour, minute)
                .toInstant(tz)
        }

        fun parseDate(
            d: Int,
            tz: TimeZone,
        ): Instant? {
            if (d == 0) return null
            return localEpoch(tz) + d.days
        }

        fun parseDatePacked(day: Int): Instant? {
            if (day == 0) return null
            val year = (day shr 9) + 2000
            val month = (day shr 5) and 0xf
            val dayOfMonth = day and 0x1f
            return LocalDate(year, month, dayOfMonth).atStartOfDayIn(TimeZone.UTC)
        }

        fun parseTimeSec(
            value: Int,
            tz: TimeZone,
        ): Instant? {
            if (value == 0) return null
            return utcEpoch() + value.toLong().seconds
        }

        fun parseTimeSecLocal(
            sec: Int,
            tz: TimeZone,
        ): Instant? {
            if (sec == 0) return null
            return localEpoch(tz) + sec.toLong().seconds
        }

        fun parseDateBCD(date: Int): Instant? {
            if (date <= 0) return null
            val year = convertBCDtoInteger(date shr 16)
            val month = convertBCDtoInteger((date shr 8) and 0xff)
            val day = convertBCDtoInteger(date and 0xff)
            return LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC)
        }

        private fun convertBCDtoInteger(bcd: Int): Int {
            var result = 0
            var shift = 0
            var remaining = bcd
            while (remaining > 0) {
                result += (remaining and 0xf) * pow10(shift)
                remaining = remaining shr 4
                shift++
            }
            return result
        }

        private fun pow10(n: Int): Int {
            var result = 1
            repeat(n) { result *= 10 }
            return result
        }

        fun date(name: String) = En1545FixedInteger(dateName(name), 14)

        fun datePacked(name: String) = En1545FixedInteger(datePackedName(name), 14)

        fun dateBCD(name: String) = En1545FixedInteger(dateBCDName(name), 32)

        fun time(name: String) = En1545FixedInteger(timeName(name), 11)

        fun timePacked16(name: String) = En1545FixedInteger(timePacked16Name(name), 16)

        fun timePacked11Local(name: String) = En1545FixedInteger(timePacked11LocalName(name), 11)

        fun dateTime(name: String) = En1545FixedInteger(dateTimeName(name), 30)

        fun dateTimeLocal(name: String) = En1545FixedInteger(dateTimeLocalName(name), 30)

        fun timeLocal(name: String) = En1545FixedInteger(timeLocalName(name), 11)
    }
}
