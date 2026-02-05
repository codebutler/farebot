/*
 * OVChipIndex.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc

import com.codebutler.farebot.base.util.ByteUtils
import kotlinx.serialization.Serializable

@Serializable
data class OVChipIndex(
    /** Most recent transaction slot (0xFB0 or 0xFD0) */
    val recentTransactionSlot: Int,
    /** Most recent card information index slot (0x5C0 or 0x580) */
    val recentInfoSlot: Int,
    /** Most recent subscription index slot (0xF10 or 0xF30) */
    val recentSubscriptionSlot: Int,
    /** Most recent travel history index slot (0xF50 or 0xF70) */
    val recentTravelhistorySlot: Int,
    /** Most recent credit index slot (0xF90 or 0xFA0) */
    val recentCreditSlot: Int,
    val subscriptionIndex: IntArray
) {
    companion object {
        fun create(data: ByteArray): OVChipIndex {
            val firstSlot = data.copyOfRange(0, data.size / 2)
            val secondSlot = data.copyOfRange(data.size / 2, data.size)

            val iIDa3 = ((firstSlot[1].toInt() and 0x3F) shl 10) or ((firstSlot[2].toInt() and 0xFF) shl 2) or
                    ((firstSlot[3].toInt() shr 6) and 0x03)
            val iIDb3 = ((secondSlot[1].toInt() and 0x3F) shl 10) or ((secondSlot[2].toInt() and 0xFF) shl 2) or
                    ((secondSlot[3].toInt() shr 6) and 0x03)

            val recentTransactionSlot = if (iIDb3 > iIDa3) 0xFB0 else 0xFD0
            val buffer = if (iIDb3 > iIDa3) secondSlot else firstSlot

            val cardindex = (buffer[3].toInt() shr 5) and 0x01
            val recentInfoSlot = if (cardindex == 1) 0x5C0 else 0x580

            val indexes = (buffer[31].toInt() shr 5) and 0x07
            val recentSubscriptionSlot = if ((indexes and 0x04) == 0x00) 0xF10 else 0xF30
            val recentTravelhistorySlot = if ((indexes and 0x02) == 0x00) 0xF50 else 0xF70
            val recentCreditSlot = if ((indexes and 0x01) == 0x00) 0xF90 else 0xFA0

            val subscriptionIndex = IntArray(12)
            val offset = 108

            for (i in 0 until 12) {
                val bits = ByteUtils.getBitsFromBuffer(buffer, offset + (i * 4), 4)
                subscriptionIndex[i] = when {
                    bits < 5 -> 0x800 + bits * 0x30
                    bits > 9 -> 0xA00 + (bits - 10) * 0x30
                    else -> 0x900 + (bits - 5) * 0x30
                }
            }

            return OVChipIndex(
                recentTransactionSlot,
                recentInfoSlot,
                recentSubscriptionSlot,
                recentTravelhistorySlot,
                recentCreditSlot,
                subscriptionIndex
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OVChipIndex) return false
        return recentTransactionSlot == other.recentTransactionSlot &&
                recentInfoSlot == other.recentInfoSlot &&
                recentSubscriptionSlot == other.recentSubscriptionSlot &&
                recentTravelhistorySlot == other.recentTravelhistorySlot &&
                recentCreditSlot == other.recentCreditSlot &&
                subscriptionIndex.contentEquals(other.subscriptionIndex)
    }

    override fun hashCode(): Int {
        var result = recentTransactionSlot
        result = 31 * result + recentInfoSlot
        result = 31 * result + recentSubscriptionSlot
        result = 31 * result + recentTravelhistorySlot
        result = 31 * result + recentCreditSlot
        result = 31 * result + subscriptionIndex.contentHashCode()
        return result
    }
}
