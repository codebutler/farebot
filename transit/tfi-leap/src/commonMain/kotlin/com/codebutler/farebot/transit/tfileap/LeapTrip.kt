/*
 * LeapTrip.kt
 *
 * Copyright 2018-2019 Google
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

package com.codebutler.farebot.transit.tfileap

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.mdst.TransportType
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

class LeapTrip internal constructor(
    private val mAgency: Int,
    private var mMode: Mode?,
    private var mStart: LeapTripPoint?,
    private var mEnd: LeapTripPoint?,
) : Trip(),
    Comparable<LeapTrip> {
    private val timestamp: Instant?
        get() = mStart?.timestamp ?: mEnd?.timestamp

    override val startTimestamp: Instant?
        get() = mStart?.timestamp

    override val endTimestamp: Instant?
        get() = mEnd?.timestamp

    override val startStation: Station?
        get() {
            val s = mStart?.station ?: return null
            return lookupStation((mAgency shl 16) or s)
        }

    override val endStation: Station?
        get() {
            val s = mEnd?.station ?: return null
            return lookupStation((mAgency shl 16) or s)
        }

    override val fare: TransitCurrency?
        get() {
            var amount = mStart?.amount ?: return null
            amount += mEnd?.amount ?: 0
            return TransitCurrency.EUR(amount)
        }

    override val mode: Mode
        get() = mMode ?: guessMode(mAgency)

    override val agencyName: FormattedString?
        get() = MdstStationLookup.getOperatorName(LEAP_STR, mAgency)?.let { FormattedString(it) }

    override val shortAgencyName: FormattedString?
        get() = agencyName

    override fun compareTo(other: LeapTrip): Int {
        val timestamp = timestamp ?: return -1
        return timestamp.compareTo(other.timestamp ?: return +1)
    }

    private fun isMergeable(leapTrip: LeapTrip): Boolean =
        (mAgency == leapTrip.mAgency) &&
            valuesCompatible(mMode, leapTrip.mMode) &&
            mStart?.isMergeable(leapTrip.mStart) != false

    private fun merge(trip: LeapTrip) {
        mStart = LeapTripPoint.merge(mStart, trip.mStart)
        mEnd = LeapTripPoint.merge(mEnd, trip.mEnd)
        if (mMode == null) {
            mMode = trip.mMode
        }
    }

    private fun lookupStation(stationId: Int): Station? {
        val result = MdstStationLookup.getStation(LEAP_STR, stationId) ?: return null
        return Station(
            stationName = result.stationName,
            shortStationName = result.shortStationName,
            companyName = result.companyName,
            lineNames = result.lineNames,
            latitude = if (result.hasLocation) result.latitude else null,
            longitude = if (result.hasLocation) result.longitude else null,
        )
    }

    companion object {
        internal const val LEAP_STR = "tfi_leap"

        private fun guessMode(anum: Int): Mode {
            val transportType = MdstStationLookup.getOperatorDefaultMode(LEAP_STR, anum)
            return when (transportType) {
                TransportType.BUS -> Mode.BUS
                TransportType.TRAIN -> Mode.TRAIN
                TransportType.TRAM -> Mode.TRAM
                TransportType.METRO -> Mode.METRO
                TransportType.FERRY -> Mode.FERRY
                TransportType.TICKET_MACHINE -> Mode.TICKET_MACHINE
                TransportType.TROLLEYBUS -> Mode.TROLLEYBUS
                TransportType.MONORAIL -> Mode.MONORAIL
                else -> Mode.OTHER
            }
        }

        private const val EVENT_CODE_BOARD = 0xb
        private const val EVENT_CODE_OUT = 0xc

        fun parseTopup(
            file: ByteArray,
            offset: Int,
        ): LeapTrip? {
            if (isNull(file, offset, 9)) {
                return null
            }

            // 3 bytes serial
            val c = LeapTransitInfo.parseDate(file, offset + 3)
            val agency = file.byteArrayToInt(offset + 7, 2)
            // 2 bytes agency again
            // 2 bytes unknown
            // 1 byte counter
            val amount = LeapTransitInfo.parseBalance(file, offset + 0xe)
            return if (amount == 0) {
                null
            } else {
                LeapTrip(
                    agency,
                    Mode.TICKET_MACHINE,
                    LeapTripPoint(c, -amount, -1, null),
                    null,
                )
            }
            // 3 bytes amount after topup: we have currently no way to represent it
        }

        private fun isNull(
            data: ByteArray,
            offset: Int,
            length: Int,
        ): Boolean = data.sliceOffLen(offset, length).isAllZero()

        fun parsePurseTrip(
            file: ByteArray,
            offset: Int,
        ): LeapTrip? {
            if (isNull(file, offset, 7)) {
                return null
            }

            val eventCode = file[offset].toInt() and 0xff
            val c = LeapTransitInfo.parseDate(file, offset + 1)
            val amount = LeapTransitInfo.parseBalance(file, offset + 5)
            // 3 bytes unknown
            val agency = file.byteArrayToInt(offset + 0xb, 2)
            // 2 bytes unknown
            // 1 byte counter
            val event = LeapTripPoint(c, amount, eventCode, null)
            return if (eventCode == EVENT_CODE_OUT) {
                LeapTrip(
                    agency,
                    null,
                    null,
                    event,
                )
            } else {
                LeapTrip(
                    agency,
                    null,
                    event,
                    null,
                )
            }
        }

        fun parseTrip(
            file: ByteArray,
            offset: Int,
        ): LeapTrip? {
            if (isNull(file, offset, 7)) {
                return null
            }

            val eventCode2 = file[offset].toInt() and 0xff
            val eventTime = LeapTransitInfo.parseDate(file, offset + 1)
            val agency = file.byteArrayToInt(offset + 5, 2)
            // 0xd bytes unknown
            val amount = LeapTransitInfo.parseBalance(file, offset + 0x14)
            // 3 bytes balance after event
            // 0x22 bytes unknown
            val eventCode = file[offset + 0x39].toInt() and 0xff
            // 8 bytes unknown
            val from = file.byteArrayToInt(offset + 0x42, 2)
            val to = file.byteArrayToInt(offset + 0x44, 2)
            // 0x10 bytes unknown
            val startTime = LeapTransitInfo.parseDate(file, offset + 0x56)
            // 0x27 bytes unknown
            var mode: Mode? = null
            val start: LeapTripPoint
            var end: LeapTripPoint? = null
            when (eventCode2) {
                0x04 -> {
                    mode = Mode.TICKET_MACHINE
                    start = LeapTripPoint(eventTime, -amount, -1, if (from == 0) null else from)
                }
                0xce -> {
                    start = LeapTripPoint(startTime, null, null, from)
                    end = LeapTripPoint(eventTime, -amount, eventCode, to)
                }
                0xca -> start = LeapTripPoint(eventTime, -amount, eventCode, from)
                else -> start = LeapTripPoint(eventTime, -amount, eventCode, from)
            }
            return LeapTrip(agency, mode, start, end)
        }

        fun postprocess(trips: Iterable<LeapTrip?>): List<LeapTrip> {
            val srt = trips.filterNotNull().sorted()
            val merged = mutableListOf<LeapTrip>()
            for (trip in srt) {
                if (merged.isEmpty()) {
                    merged.add(trip)
                    continue
                }
                if (merged[merged.size - 1].isMergeable(trip)) {
                    merged[merged.size - 1].merge(trip)
                    continue
                }
                merged.add(trip)
            }
            return merged
        }
    }
}
