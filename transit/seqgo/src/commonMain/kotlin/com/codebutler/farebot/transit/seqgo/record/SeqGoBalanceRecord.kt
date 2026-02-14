/*
 * SeqGoBalanceRecord.kt
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
import kotlinx.serialization.Serializable

/**
 * Represents balance records on Go card
 * https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29#balance-record-type
 */
@Serializable
data class SeqGoBalanceRecord(
    /** The balance of the card, in cents. */
    val balance: Int,
    val version: Int,
) : SeqGoRecord(),
    Comparable<SeqGoBalanceRecord> {
    override fun compareTo(other: SeqGoBalanceRecord): Int {
        // So sorting works, we reverse the order so highest number is first.
        return other.version.compareTo(this.version)
    }

    companion object {
        fun recordFromBytes(input: ByteArray): SeqGoBalanceRecord {
            if (input[0] != 0x01.toByte()) {
                throw AssertionError()
            }

            val balance = ByteUtils.byteArrayToInt(ByteUtils.reverseBuffer(input, 2, 2), 0, 2)
            val version = ByteUtils.byteArrayToInt(input, 13, 1)

            return SeqGoBalanceRecord(balance, version)
        }
    }
}
