/*
 * SeqGoTapRecord.kt
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

package com.codebutler.farebot.transit.seqgo.record

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.seqgo.SeqGoData
import com.codebutler.farebot.transit.seqgo.SeqGoDateUtil
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Tap record type
 * https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29#tap-record-type
 */
@Serializable
data class SeqGoTapRecord(
    private val modeData: Int,
    val timestamp: Instant,
    val journey: Int,
    val station: Int,
    val checksum: Int,
) : SeqGoRecord(),
    Comparable<SeqGoTapRecord> {
    val mode: Trip.Mode
        get() = SeqGoData.VEHICLES[modeData] ?: Trip.Mode.OTHER

    override fun compareTo(other: SeqGoTapRecord): Int {
        // Group by journey, then by timestamp.
        return if (other.journey == this.journey) {
            this.timestamp.compareTo(other.timestamp)
        } else {
            integerCompare(this.journey, other.journey)
        }
    }

    companion object {
        private fun integerCompare(
            lhs: Int,
            rhs: Int,
        ): Int =
            if (lhs < rhs) {
                -1
            } else if (lhs == rhs) {
                0
            } else {
                1
            }

        fun recordFromBytes(input: ByteArray): SeqGoTapRecord {
            if (input[0] != 0x31.toByte()) {
                throw AssertionError("not a tap record")
            }

            val mode = ByteUtils.byteArrayToInt(input, 1, 1)
            val timestamp = SeqGoDateUtil.unpackDate(ByteUtils.reverseBuffer(input, 2, 4))
            val journey = ByteUtils.byteArrayToInt(ByteUtils.reverseBuffer(input, 5, 2)) shr 3
            val station = ByteUtils.byteArrayToInt(ByteUtils.reverseBuffer(input, 12, 2))
            val checksum = ByteUtils.byteArrayToInt(ByteUtils.reverseBuffer(input, 14, 2))

            return SeqGoTapRecord(mode, timestamp, journey, station, checksum)
        }
    }
}
