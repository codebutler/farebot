/*
 * OVChipCredit.kt
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
data class OVChipCredit(
    val id: Int,
    val creditId: Int,
    val credit: Int,
    val banbits: Int
) {
    companion object {
        fun create(data: ByteArray?): OVChipCredit {
            val d = data ?: ByteArray(16)

            val banbits = ByteUtils.getBitsFromBuffer(d, 0, 9)
            val id = ByteUtils.getBitsFromBuffer(d, 9, 12)
            val creditId = ByteUtils.getBitsFromBuffer(d, 56, 12)
            var credit = ByteUtils.getBitsFromBuffer(d, 78, 15)

            if ((d[9].toInt() and 0x04) != 4) {
                credit = credit xor 0x7FFF
                credit *= -1
            }

            return OVChipCredit(id, creditId, credit, banbits)
        }
    }
}
