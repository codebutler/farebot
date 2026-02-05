/*
 * OVChipPreamble.kt
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
data class OVChipPreamble(
    val id: String,
    val checkbit: Int,
    val manufacturer: String,
    val publisher: String,
    val unknownConstant1: String,
    val expdate: Int,
    val unknownConstant2: String,
    val type: Int
) {
    companion object {
        fun create(data: ByteArray?): OVChipPreamble {
            val d = data ?: ByteArray(48)

            val hex = ByteUtils.getHexString(d)

            val id = hex.substring(0, 8)
            val checkbit = ByteUtils.getBitsFromBuffer(d, 32, 8)
            val manufacturer = hex.substring(10, 20)
            val publisher = hex.substring(20, 32)
            val unknownConstant1 = hex.substring(32, 54)
            val expdate = ByteUtils.getBitsFromBuffer(d, 216, 20)
            val unknownConstant2 = hex.substring(59, 68)
            val type = ByteUtils.getBitsFromBuffer(d, 276, 4)

            return OVChipPreamble(id, checkbit, manufacturer, publisher, unknownConstant1, expdate, unknownConstant2, type)
        }
    }
}
