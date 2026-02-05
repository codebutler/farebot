/*
 * VeneziaTransaction.kt
 *
 * Copyright 2018-2019 Google
 * Copyright 2025 Eric Butler
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

package com.codebutler.farebot.transit.calypso.venezia

import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Subscription
import com.codebutler.farebot.transit.en1545.En1545Transaction
import com.codebutler.farebot.transit.en1545.En1545TransitData

internal class VeneziaTransaction(
    override val parsed: En1545Parsed
) : En1545Transaction() {

    override val lookup get() = VeneziaLookup

    override val mode: Trip.Mode
        get() {
            val transportType = parsed.getInt("TransportType")
            val y = parsed.getInt("Y")

            return when {
                transportType == 1 -> Trip.Mode.BUS
                transportType == 5 -> Trip.Mode.FERRY
                y == 1000 -> Trip.Mode.FERRY
                else -> Trip.Mode.BUS
            }
        }

    companion object {
        private val TRIP_FIELDS = En1545Container(
            En1545Container(
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_A + "1", 1),
                En1545FixedInteger(En1545TransitData.CONTRACTS_TARIFF + "1", 16),
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_B + "1", 1)
            ),
            En1545Container(
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_A + "2", 1),
                En1545FixedInteger(En1545TransitData.CONTRACTS_TARIFF + "2", 16),
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_B + "2", 1)
            ),
            En1545Container(
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_A + "3", 1),
                En1545FixedInteger(En1545TransitData.CONTRACTS_TARIFF + "3", 16),
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_B + "3", 1)
            ),
            En1545Container(
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_A + "4", 1),
                En1545FixedInteger(En1545TransitData.CONTRACTS_TARIFF + "4", 16),
                En1545FixedInteger(En1545TransitData.CONTRACTS_UNKNOWN_B + "4", 1)
            ),
            En1545FixedInteger("EventUnknownA", 1),
            En1545FixedInteger(En1545Transaction.EVENT_CONTRACT_TARIFF, 16),
            En1545FixedInteger("EventUnknownB", 4),
            En1545FixedInteger.datePacked(En1545Transaction.EVENT),
            En1545FixedInteger.timePacked11Local(En1545Transaction.EVENT_FIRST_STAMP),
            En1545FixedInteger.timePacked11Local(En1545Transaction.EVENT),
            En1545FixedInteger("EventUnknownC", 9),
            En1545FixedInteger("TransportType", 4),
            En1545FixedInteger("Y", 14),
            En1545FixedInteger("Z", 16),
            En1545FixedInteger("EventUnknownE", 18),
            En1545FixedInteger("PreviousZ", 16),
            En1545FixedInteger("EventUnknownF", 26)
        )

        fun parse(data: ByteArray): VeneziaTransaction? {
            if (data.drop(9).all { it == 0.toByte() }) {
                return null
            }

            val parsed = En1545Parser.parse(data, TRIP_FIELDS)
            return VeneziaTransaction(parsed)
        }
    }
}
