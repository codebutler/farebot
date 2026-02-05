/*
 * SeqGoUtil.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.seq_go

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.transit.Station
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Misc utilities for parsing Go Cards
 *
 * @author Michael Farrell
 */
object SeqGoUtil {

    private const val SEQ_GO_STR = "seq_go"

    /**
     * Date format:
     *
     * 0001111 1100 00100 = 2015-12-04
     * yyyyyyy mmmm ddddd
     *
     * Bottom 11 bits = minutes since 00:00
     * Time is represented in localtime, Australia/Brisbane.
     *
     * Assumes that data has already been byte-reversed for big endian parsing.
     *
     * @param timestamp Four bytes of input representing the timestamp to parse
     * @return Date and time represented by that value
     */
    fun unpackDate(timestamp: ByteArray): Instant {
        val minute = ByteUtils.getBitsFromBuffer(timestamp, 5, 11)
        val year = ByteUtils.getBitsFromBuffer(timestamp, 16, 7) + 2000
        val month = ByteUtils.getBitsFromBuffer(timestamp, 23, 4)
        val day = ByteUtils.getBitsFromBuffer(timestamp, 27, 5)

        if (minute > 1440) {
            throw AssertionError("Minute > 1440")
        }
        if (minute < 0) {
            throw AssertionError("Minute < 0")
        }

        if (day > 31) {
            throw AssertionError("Day > 31")
        }
        if (month > 12) {
            throw AssertionError("Month > 12")
        }

        val hour = minute / 60
        val min = minute % 60
        return LocalDateTime(year, month, day, hour, min).toInstant(TimeZone.of("Australia/Brisbane"))
    }

    fun getStation(stationId: Int): Station? {
        if (stationId == 0) {
            return null
        }

        val result = MdstStationLookup.getStation(SEQ_GO_STR, stationId) ?: return null

        return Station.builder()
            .stationName(result.stationName)
            .latitude(if (result.hasLocation) result.latitude.toString() else null)
            .longitude(if (result.hasLocation) result.longitude.toString() else null)
            .build()
    }
}
