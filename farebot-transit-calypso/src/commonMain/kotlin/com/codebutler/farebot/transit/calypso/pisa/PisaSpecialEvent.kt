/*
 * PisaSpecialEvent.kt
 *
 * Copyright 2018-2019 Google
 * Copyright 2025 Eric Butler
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

package com.codebutler.farebot.transit.calypso.pisa

import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Transaction

internal class PisaSpecialEvent(
    override val parsed: En1545Parsed
) : En1545Transaction() {

    override val lookup get() = PisaLookup

    companion object {
        private val SPECIAL_EVENT_FIELDS = En1545Container(
            En1545FixedInteger("EventUnknownA", 13),
            En1545FixedInteger.date(En1545Transaction.EVENT),
            En1545FixedInteger.timeLocal(En1545Transaction.EVENT),
            En1545FixedHex("EventUnknownB", 0x1d * 8 - 14 - 11 - 13)
        )

        fun parse(data: ByteArray): PisaSpecialEvent? {
            val parsed = En1545Parser.parse(data, SPECIAL_EVENT_FIELDS)
            return PisaSpecialEvent(parsed)
        }
    }
}
