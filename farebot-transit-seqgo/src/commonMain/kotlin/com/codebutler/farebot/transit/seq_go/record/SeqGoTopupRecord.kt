/*
 * SeqGoTopupRecord.kt
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

package com.codebutler.farebot.transit.seq_go.record

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.transit.seq_go.SeqGoDateUtil
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Top-up record type
 * https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29#top-up-record-type
 */
@Serializable
data class SeqGoTopupRecord(
    val timestamp: Instant,
    val credit: Int,
    val station: Int,
    val checksum: Int,
    val automatic: Boolean
) : SeqGoRecord() {

    companion object {
        fun recordFromBytes(input: ByteArray): SeqGoTopupRecord {
            if ((input[0] != 0x01.toByte() && input[0] != 0x31.toByte()) || input[1] != 0x01.toByte()) {
                throw AssertionError("Not a topup record")
            }

            val timestamp = SeqGoDateUtil.unpackDate(ByteUtils.reverseBuffer(input, 2, 4))
            val credit = ByteUtils.byteArrayToInt(ByteUtils.reverseBuffer(input, 6, 2))
            val station = ByteUtils.byteArrayToInt(ByteUtils.reverseBuffer(input, 12, 2))
            val checksum = ByteUtils.byteArrayToInt(ByteUtils.reverseBuffer(input, 14, 2))
            val automatic = input[0] == 0x31.toByte()
            return SeqGoTopupRecord(timestamp, credit, station, checksum, automatic)
        }
    }
}
