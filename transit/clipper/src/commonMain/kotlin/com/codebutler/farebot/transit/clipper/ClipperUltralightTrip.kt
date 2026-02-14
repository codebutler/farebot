/*
 * ClipperUltralightTrip.kt
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

package com.codebutler.farebot.transit.clipper

import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

class ClipperUltralightTrip(
    private val time: Int,
    private val transferExpiry: Int,
    private val seqCounter: Int,
    val tripsRemaining: Int,
    private val balanceSeqCounter: Int,
    private val station: Int,
    private val type: Int,
    private val agency: Int,
) : Trip() {
    constructor(transaction: ByteArray, baseDate: Int) : this(
        seqCounter = getBitsFromBuffer(transaction, 0, 7),
        type = getBitsFromBuffer(transaction, 7, 17),
        time = baseDate * 1440 - getBitsFromBuffer(transaction, 24, 17),
        station = getBitsFromBuffer(transaction, 41, 17),
        agency = getBitsFromBuffer(transaction, 68, 5),
        balanceSeqCounter = getBitsFromBuffer(transaction, 80, 4),
        tripsRemaining = getBitsFromBuffer(transaction, 84, 6),
        transferExpiry = getBitsFromBuffer(transaction, 100, 10),
    )

    val isHidden: Boolean get() = type == 1

    val transferExpiryTime: Int get() = if (transferExpiry == 0) 0 else transferExpiry + time

    override val startTimestamp: Instant
        get() = Instant.fromEpochSeconds(ClipperUtil.clipperTimestampToEpochSeconds(time * 60L))

    override val startStation: Station?
        get() = ClipperData.getStation(agency, station, false)

    override val agencyName: String
        get() = ClipperData.getAgencyName(agency)

    override val shortAgencyName: String
        get() = ClipperData.getShortAgencyName(agency)

    override val mode: Mode get() = ClipperData.getMode(agency)

    fun isSeqGreater(other: ClipperUltralightTrip): Boolean =
        if (other.balanceSeqCounter != balanceSeqCounter) {
            (balanceSeqCounter - other.balanceSeqCounter) and 0x8 == 0
        } else {
            (seqCounter - other.seqCounter) and 0x40 == 0
        }

    companion object {
        private fun getBitsFromBuffer(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            var result = 0
            for (i in offset until offset + length) {
                result = result shl 1
                val byteIndex = i / 8
                val bitIndex = 7 - (i % 8)
                if (byteIndex < buffer.size && (buffer[byteIndex].toInt() shr bitIndex) and 1 == 1) {
                    result = result or 1
                }
            }
            return result
        }
    }
}
