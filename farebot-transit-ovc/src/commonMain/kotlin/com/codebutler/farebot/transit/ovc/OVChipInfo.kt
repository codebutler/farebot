/*
 * OVChipInfo.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012, 2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.serialization.Serializable

@Serializable
data class OVChipInfo(
    val company: Int,
    val expdate: Int,
    val birthdate: Instant,
    val active: Int,
    val limit: Int,
    val charge: Int,
    val unknown: Int
) {
    companion object {
        fun create(data: ByteArray?): OVChipInfo {
            val d = data ?: ByteArray(48)

            val company: Int
            val expdate: Int
            var birthdate: Instant = Clock.System.now()
            var active = 0
            var limit = 0
            var charge = 0
            var unknown = 0

            company = ((d[6].toInt() and 0xFF) shr 3) and 0x1F
            expdate = (((d[6].toInt() and 0xFF) and 0x07) shl 11) or
                    (((d[7].toInt() and 0xFF)) shl 3) or
                    (((d[8].toInt() and 0xFF) shr 5) and 0x07)

            if ((d[13].toInt() and 0x02) == 0x02) {
                val year = (ByteUtils.convertBCDtoInteger(d[14]) * 100) + ByteUtils.convertBCDtoInteger(d[15])
                val month = ByteUtils.convertBCDtoInteger(d[16])
                val day = ByteUtils.convertBCDtoInteger(d[17])

                birthdate = LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC)

                active = (d[22].toInt() shr 5) and 0x07
                limit = (((d[22].toInt() and 0xFF) and 0x1F) shl 11) or (((d[23].toInt() and 0xFF)) shl 3) or
                        (((d[24].toInt() and 0xFF) shr 5) and 0x07)
                charge = (((d[24].toInt() and 0xFF) and 0x1F) shl 11) or (((d[25].toInt() and 0xFF)) shl 3) or
                        (((d[26].toInt() and 0xFF) shr 5) and 0x07)
                unknown = (((d[26].toInt() and 0xFF) and 0x1F) shl 11) or (((d[27].toInt() and 0xFF)) shl 3) or
                        (((d[28].toInt() and 0xFF) shr 5) and 0x07)
            }

            return OVChipInfo(company, expdate, birthdate, active, limit, charge, unknown)
        }
    }
}
