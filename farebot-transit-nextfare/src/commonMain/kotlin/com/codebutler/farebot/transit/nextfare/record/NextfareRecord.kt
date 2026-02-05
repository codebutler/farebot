/*
 * NextfareRecord.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.nextfare.record

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Represents a record on a Nextfare (Cubic) card.
 * Fans out parsing to subclasses based on sector/block position.
 *
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
interface NextfareRecord {
    companion object {
        fun recordFromBytes(
            input: ByteArray,
            sectorIndex: Int,
            blockIndex: Int,
            timeZone: TimeZone
        ): NextfareRecord? {
            return when {
                sectorIndex == 1 && blockIndex <= 1 ->
                    NextfareBalanceRecord.recordFromBytes(input)
                sectorIndex == 1 && blockIndex == 2 ->
                    NextfareConfigRecord.recordFromBytes(input, timeZone)
                sectorIndex == 2 ->
                    NextfareTopupRecord.recordFromBytes(input, timeZone)
                sectorIndex == 3 ->
                    NextfareTravelPassRecord.recordFromBytes(input, timeZone)
                sectorIndex in 5..8 ->
                    NextfareTransactionRecord.recordFromBytes(input, timeZone)
                else -> null
            }
        }

        /**
         * Unpack Nextfare date/time format.
         *
         * Top two bytes: yyyyyyy mmmm ddddd (year + 2000, month, day)
         * Bottom 11 bits: minutes since midnight
         *
         * Little-endian 4-byte integer.
         */
        fun unpackDate(input: ByteArray, offset: Int, timeZone: TimeZone): Instant {
            val timestamp = byteArrayToIntReversed(input, offset, 4)
            val minute = getBitsFromInteger(timestamp, 16, 11)
            val year = getBitsFromInteger(timestamp, 9, 7) + 2000
            val month = getBitsFromInteger(timestamp, 5, 4)
            val day = getBitsFromInteger(timestamp, 0, 5)

            require(minute in 0..1440) { "Invalid minute: $minute" }
            require(day in 1..31) { "Invalid day: $day" }
            require(month in 1..12) { "Invalid month: $month" }

            val ldt = LocalDateTime(year, month, day, minute / 60, minute % 60, 0)
            return ldt.toInstant(timeZone)
        }

        fun byteArrayToIntReversed(data: ByteArray, offset: Int, length: Int): Int {
            var result = 0
            for (i in 0 until length) {
                result = result or ((data[offset + i].toInt() and 0xFF) shl (i * 8))
            }
            return result
        }

        fun byteArrayToLongReversed(data: ByteArray, offset: Int, length: Int): Long {
            var result = 0L
            for (i in 0 until length) {
                result = result or ((data[offset + i].toLong() and 0xFF) shl (i * 8))
            }
            return result
        }

        fun byteArrayToInt(data: ByteArray, offset: Int, length: Int): Int {
            var result = 0
            for (i in 0 until length) {
                result = (result shl 8) or (data[offset + i].toInt() and 0xFF)
            }
            return result
        }

        fun byteArrayToLong(data: ByteArray, offset: Int, length: Int): Long {
            var result = 0L
            for (i in 0 until length) {
                result = (result shl 8) or (data[offset + i].toLong() and 0xFF)
            }
            return result
        }

        private fun getBitsFromInteger(value: Int, startBit: Int, length: Int): Int {
            return (value shr startBit) and ((1 shl length) - 1)
        }
    }
}
