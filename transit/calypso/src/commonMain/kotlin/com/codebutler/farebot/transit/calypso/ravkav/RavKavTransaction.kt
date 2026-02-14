/*
 * RavKavTransaction.kt
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

package com.codebutler.farebot.transit.calypso.ravkav

import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Transaction

internal class RavKavTransaction(
    override val parsed: En1545Parsed,
) : En1545Transaction() {
    override val lookup: En1545Lookup
        get() = RavKavLookup

    fun shouldBeDropped(): Boolean = eventType == EVENT_TYPE_CANCELLED

    companion object {
        private const val EVENT = "Event"
        private const val EVENT_SERVICE_PROVIDER = "EventServiceProvider"
        private const val EVENT_CONTRACT_POINTER = "EventContractPointer"
        private const val EVENT_CODE = "EventCode"
        private const val EVENT_TRANSFER_FLAG = "EventTransferFlag"
        private const val EVENT_FIRST_STAMP = "EventFirstStamp"
        private const val EVENT_CONTRACT_PREFS = "EventContractPrefs"
        private const val EVENT_LOCATION_ID = "EventLocationId"
        private const val EVENT_ROUTE_NUMBER = "EventRouteNumber"
        private const val STOP_EN_ROUTE = "StopEnRoute"
        private const val EVENT_UNKNOWN_A = "EventUnknownA"
        private const val EVENT_VEHICLE_ID = "EventVehicleId"
        private const val EVENT_UNKNOWN_B = "EventUnknownB"
        private const val EVENT_UNKNOWN_C = "EventUnknownC"
        private const val ROUTE_SYSTEM = "RouteSystem"
        private const val FARE_CODE = "FareCode"
        private const val EVENT_PRICE_AMOUNT = "EventPriceAmount"
        private const val EVENT_UNKNOWN_D = "EventUnknownD"
        private const val EVENT_UNKNOWN_E = "EventUnknownE"

        val FIELDS =
            En1545Container(
                En1545FixedInteger("EventVersion", 3),
                En1545FixedInteger(EVENT_SERVICE_PROVIDER, 8),
                En1545FixedInteger(EVENT_CONTRACT_POINTER, 4),
                En1545FixedInteger(EVENT_CODE, 8),
                En1545FixedInteger.dateTime(EVENT),
                En1545FixedInteger(EVENT_TRANSFER_FLAG, 1),
                En1545FixedInteger.dateTime(EVENT_FIRST_STAMP),
                En1545FixedInteger(EVENT_CONTRACT_PREFS, 32),
                En1545Bitmap(
                    En1545FixedInteger(EVENT_LOCATION_ID, 16),
                    En1545FixedInteger(EVENT_ROUTE_NUMBER, 16),
                    En1545FixedInteger(STOP_EN_ROUTE, 8),
                    En1545FixedInteger(EVENT_UNKNOWN_A, 12),
                    En1545FixedInteger(EVENT_VEHICLE_ID, 14),
                    En1545FixedInteger(EVENT_UNKNOWN_B, 4),
                    En1545FixedInteger(EVENT_UNKNOWN_C, 8),
                ),
                En1545Bitmap(
                    En1545Container(
                        En1545FixedInteger(ROUTE_SYSTEM, 10),
                        En1545FixedInteger(FARE_CODE, 8),
                        En1545FixedInteger(EVENT_PRICE_AMOUNT, 16),
                    ),
                    En1545FixedInteger(EVENT_UNKNOWN_D, 32),
                    En1545FixedInteger(EVENT_UNKNOWN_E, 32),
                ),
            )
    }
}
