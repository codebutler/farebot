/*
 * LisboaVivaTransaction.kt
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

package com.codebutler.farebot.transit.calypso.lisboaviva

import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Transaction

internal class LisboaVivaTransaction private constructor(
    override val parsed: En1545Parsed,
    override val lookup: En1545Lookup,
) : En1545Transaction() {
    override val isTapOn: Boolean
        get() = parsed.getIntOrZero(TRANSITION) == 1

    override val isTransfer: Boolean
        get() = parsed.getIntOrZero(TRANSITION) == 3

    override val isTapOff: Boolean
        get() = parsed.getIntOrZero(TRANSITION) == 4

    override val routeNames: List<String>
        get() {
            val routeNumber = parsed.getInt(EVENT_ROUTE_NUMBER) ?: return emptyList()
            if (agency == LisboaVivaLookup.AGENCY_CP && routeNumber == LisboaVivaLookup.ROUTE_CASCAIS_SADO) {
                return if ((stationId ?: 0) <= 54) {
                    listOf("Cascais")
                } else {
                    listOf("Sado")
                }
            }
            return super.routeNames
        }

    override fun getStation(station: Int?): Station? =
        station?.let {
            lookup.getStation(it, agency, parsed.getIntOrZero(EVENT_ROUTE_NUMBER))
        }

    override fun isSameTrip(other: Transaction): Boolean {
        if (other !is En1545Transaction) {
            return false
        }
        // Metro transfers don't involve tap-off/tap-on
        if (parsed.getIntOrZero(EVENT_SERVICE_PROVIDER) == LisboaVivaLookup.AGENCY_METRO &&
            other.parsed.getIntOrZero(EVENT_SERVICE_PROVIDER) == LisboaVivaLookup.AGENCY_METRO
        ) {
            return true
        }
        return super.isSameTrip(other)
    }

    companion object {
        private const val CONTRACTS_USED_BITMAP = "ContractsUsedBitmap"
        private const val TRANSITION = "Transition"

        private val TRIP_FIELDS =
            En1545Container(
                En1545FixedInteger.dateTimeLocal(EVENT),
                En1545FixedInteger(EVENT_UNKNOWN_A, 3),
                En1545FixedInteger.dateTimeLocal(EVENT_FIRST_STAMP),
                En1545FixedInteger(EVENT_UNKNOWN_B, 5),
                En1545FixedInteger(CONTRACTS_USED_BITMAP, 4),
                En1545FixedHex(EVENT_UNKNOWN_C, 29),
                En1545FixedInteger(TRANSITION, 3),
                En1545FixedInteger(EVENT_SERVICE_PROVIDER, 5),
                En1545FixedInteger(EVENT_VEHICLE_ID, 16),
                En1545FixedInteger(EVENT_UNKNOWN_D, 4),
                En1545FixedInteger(EVENT_DEVICE_ID, 16),
                En1545FixedInteger(EVENT_ROUTE_NUMBER, 16),
                En1545FixedInteger(EVENT_LOCATION_ID, 8),
                En1545FixedHex(EVENT_UNKNOWN_E, 63),
            )

        fun parse(data: ByteArray): LisboaVivaTransaction? {
            if (data.all { it == 0.toByte() }) return null
            val parsed = En1545Parser.parse(data, TRIP_FIELDS)
            return LisboaVivaTransaction(parsed, LisboaVivaLookup)
        }
    }
}
