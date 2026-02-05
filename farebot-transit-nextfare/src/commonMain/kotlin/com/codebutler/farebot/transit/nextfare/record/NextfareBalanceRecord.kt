/*
 * NextfareBalanceRecord.kt
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

data class NextfareBalanceRecord(
    val version: Int,
    val balance: Int,
    val hasTravelPassAvailable: Boolean
) : NextfareRecord, Comparable<NextfareBalanceRecord> {

    override fun compareTo(other: NextfareBalanceRecord): Int =
        // So sorting works, we reverse the order so highest number is first.
        other.version.compareTo(this.version)

    companion object {
        fun recordFromBytes(input: ByteArray): NextfareBalanceRecord? {
            if (input.size < 16) return null

            val version = input[13].toInt() and 0xFF

            // Do some flipping for the balance
            var balance = NextfareRecord.byteArrayToIntReversed(input, 2, 2)

            // Negative balance
            if (balance and 0x8000 == 0x8000) {
                balance = balance and 0x7fff
                balance *= -1
            } else if (input[1].toInt() and 0x80 == 0x80) {
                // seq_go uses a sign flag in an adjacent byte
                balance *= -1
            }

            val hasTravelPass = input[7].toInt() != 0x00

            return NextfareBalanceRecord(version, balance, hasTravelPass)
        }
    }
}
