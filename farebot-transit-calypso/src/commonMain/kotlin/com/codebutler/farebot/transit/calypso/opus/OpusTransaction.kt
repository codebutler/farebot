/*
 * OpusTransaction.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.calypso.opus

import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Transaction

internal class OpusTransaction(
    override val parsed: En1545Parsed
) : En1545Transaction() {

    override val lookup: En1545Lookup
        get() = OpusLookup

    companion object {
        private const val EVENT = "Event"
        private const val EVENT_UNKNOWN_A = "EventUnknownA"
        private const val EVENT_UNKNOWN_B = "EventUnknownB"
        private const val EVENT_SERVICE_PROVIDER = "EventServiceProvider"
        private const val EVENT_UNKNOWN_C = "EventUnknownC"
        private const val EVENT_ROUTE_NUMBER = "EventRouteNumber"
        private const val EVENT_UNKNOWN_D = "EventUnknownD"
        private const val EVENT_UNKNOWN_E = "EventUnknownE"
        private const val EVENT_CONTRACT_POINTER = "EventContractPointer"
        private const val EVENT_FIRST_STAMP = "EventFirstStamp"
        private const val EVENT_DATA_SIMULATION = "EventDataSimulation"
        private const val EVENT_UNKNOWN_F = "EventUnknownF"
        private const val EVENT_UNKNOWN_G = "EventUnknownG"
        private const val EVENT_UNKNOWN_H = "EventUnknownH"
        private const val EVENT_UNKNOWN_I = "EventUnknownI"

        val FIELDS = En1545Container(
            En1545FixedInteger.date(EVENT),
            En1545FixedInteger.timeLocal(EVENT),
            En1545FixedInteger("UnknownX", 19),
            En1545Bitmap(
                En1545FixedInteger(EVENT_UNKNOWN_A, 8),
                En1545FixedInteger(EVENT_UNKNOWN_B, 8),
                En1545FixedInteger(EVENT_SERVICE_PROVIDER, 8),
                En1545FixedInteger(EVENT_UNKNOWN_C, 16),
                En1545FixedInteger(EVENT_ROUTE_NUMBER, 16),
                En1545FixedInteger(EVENT_UNKNOWN_D, 16),
                En1545FixedInteger(EVENT_UNKNOWN_E, 16),
                En1545FixedInteger(EVENT_CONTRACT_POINTER, 5),
                En1545Bitmap(
                    En1545FixedInteger.date(EVENT_FIRST_STAMP),
                    En1545FixedInteger.timeLocal(EVENT_FIRST_STAMP),
                    En1545FixedInteger(EVENT_DATA_SIMULATION, 1),
                    En1545FixedInteger(EVENT_UNKNOWN_F, 4),
                    En1545FixedInteger(EVENT_UNKNOWN_G, 4),
                    En1545FixedInteger(EVENT_UNKNOWN_H, 4),
                    En1545FixedInteger(EVENT_UNKNOWN_I, 4)
                )
            )
        )
    }
}
