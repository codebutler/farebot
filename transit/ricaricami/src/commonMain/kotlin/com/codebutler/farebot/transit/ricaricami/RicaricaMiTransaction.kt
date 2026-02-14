/*
 * RicaricaMiTransaction.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.ricaricami

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Transaction
import farebot.transit.ricaricami.generated.resources.*
import kotlin.time.Instant

class RicaricaMiTransaction(
    override val parsed: En1545Parsed,
    private val stringResource: StringResource,
) : En1545Transaction() {
    private val transactionType: Int
        get() = parsed.getIntOrZero(TRANSACTION_TYPE)

    override val transport get() = parsed.getIntOrZero(TRANSPORT_TYPE_B)

    override val isTapOff get() = transactionType == TRANSACTION_TAP_OFF

    override val isTapOn get() = (
        transactionType == TRANSACTION_TAP_ON ||
            transactionType == TRANSACTION_TAP_ON_TRANSFER
    )

    override val mode get(): Trip.Mode {
        if (parsed.getIntOrZero(TRANSPORT_TYPE_A) != 0 && transport != RicaricaMiLookup.TRANSPORT_BUS) {
            return Trip.Mode.OTHER
        }
        when (transport) {
            RicaricaMiLookup.TRANSPORT_BUS -> {
                if (parsed.getIntOrZero(TRANSPORT_TYPE_A) == 0) {
                    return Trip.Mode.TRAM
                }
                if (routeNumber in 90..93) {
                    return Trip.Mode.TROLLEYBUS
                }
                return Trip.Mode.BUS
            }
            RicaricaMiLookup.TRANSPORT_METRO -> return Trip.Mode.METRO
            RicaricaMiLookup.TRANSPORT_TRAM -> return Trip.Mode.TRAM
            RicaricaMiLookup.TRANSPORT_TRENORD1, RicaricaMiLookup.TRANSPORT_TRENORD2 -> return Trip.Mode.TRAIN
            else -> return Trip.Mode.OTHER
        }
    }

    override val agencyName: String?
        get() =
            when (transport) {
                RicaricaMiLookup.TRANSPORT_METRO, RicaricaMiLookup.TRANSPORT_TRAM, RicaricaMiLookup.TRANSPORT_BUS ->
                    stringResource
                        .getString(
                            Res.string.ricaricami_agency_atm,
                        )
                RicaricaMiLookup.TRANSPORT_TRENORD1 -> stringResource.getString(Res.string.ricaricami_agency_trenord_1)
                RicaricaMiLookup.TRANSPORT_TRENORD2 -> stringResource.getString(Res.string.ricaricami_agency_trenord_2)
                else -> "$transport"
            }

    override val shortAgencyName: String?
        get() =
            when (transport) {
                RicaricaMiLookup.TRANSPORT_METRO, RicaricaMiLookup.TRANSPORT_TRAM, RicaricaMiLookup.TRANSPORT_BUS ->
                    stringResource
                        .getString(
                            Res.string.ricaricami_agency_atm_short,
                        )
                RicaricaMiLookup.TRANSPORT_TRENORD1 -> stringResource.getString(Res.string.ricaricami_agency_trenord_1)
                RicaricaMiLookup.TRANSPORT_TRENORD2 -> stringResource.getString(Res.string.ricaricami_agency_trenord_2)
                else -> "$transport"
            }

    override val stationId get(): Int? {
        val id = super.stationId
        if (transport == RicaricaMiLookup.TRANSPORT_BUS && id == 999) {
            return null
        }
        return super.stationId
    }

    override val lookup get() = RicaricaMiLookup

    override fun isSameTrip(other: Transaction) =
        (
            (
                transport == RicaricaMiLookup.TRANSPORT_METRO ||
                    transport == RicaricaMiLookup.TRANSPORT_TRENORD1 ||
                    transport == RicaricaMiLookup.TRANSPORT_TRENORD2
            ) &&
                other is RicaricaMiTransaction &&
                other.transport == transport
        )

    override val timestamp get(): Instant? {
        val firstDate = parsed.getIntOrZero(En1545FixedInteger.dateName(EVENT_FIRST_STAMP))
        val firstTime = parsed.getIntOrZero(En1545FixedInteger.timeLocalName(EVENT_FIRST_STAMP))
        val time = parsed.getIntOrZero(En1545FixedInteger.timeLocalName(EVENT))
        val date = if (time < firstTime) firstDate + 1 else firstDate
        return En1545FixedInteger.parseTimeLocal(date, time, RicaricaMiLookup.TZ)
    }

    companion object {
        private const val TRANSPORT_TYPE_A = "TransportTypeA"
        private const val TRANSPORT_TYPE_B = "TransportTypeB"
        private const val TRANSACTION_TYPE = "TransactionType"
        private const val TRANSACTION_COUNTER = "TransactionCounter"
        private const val TRAIN_USED_FLAG = "TrainUsed"
        private const val TRAM_USED_FLAG = "TramUsed"
        private val TRIP_FIELDS =
            En1545Container(
                En1545FixedInteger.date(EVENT_FIRST_STAMP),
                En1545FixedInteger.timeLocal(EVENT),
                En1545FixedInteger(EVENT_UNKNOWN_A, 2),
                En1545Bitmap( // 186bd128
                    // 8
                    En1545FixedInteger("NeverSeen0", 0),
                    En1545FixedInteger("NeverSeen1", 0),
                    En1545FixedInteger("NeverSeen2", 0),
                    En1545FixedInteger(TRANSACTION_TYPE, 3),
                    // 2
                    En1545FixedInteger("NeverSeen4", 0),
                    En1545FixedInteger(EVENT_RESULT, 6), // 0 = ok
                    // 0xb = outside of urban area
                    En1545FixedInteger("NeverSeen6", 0),
                    En1545FixedInteger("NeverSeen7", 0),
                    // 1
                    En1545FixedInteger(EVENT_LOCATION_ID, 11),
                    En1545FixedInteger("NeverSeen9", 0),
                    En1545FixedInteger("NeverSeen10", 0),
                    En1545FixedInteger("NeverSeen11", 0),
                    // d
                    En1545FixedInteger(EVENT_UNKNOWN_C, 2), // Possibly gate
                    En1545FixedInteger("NeverSeen13", 0),
                    En1545FixedInteger(EVENT_ROUTE_NUMBER, 10),
                    En1545FixedInteger(EVENT_UNKNOWN_D, 12),
                    // b
                    En1545FixedInteger(TRANSPORT_TYPE_A, 1),
                    En1545FixedInteger(EVENT_VEHICLE_ID, 13),
                    En1545FixedInteger("NeverSeen18", 0),
                    En1545FixedInteger(TRANSPORT_TYPE_B, 4),
                    // 6
                    En1545FixedInteger("NeverSeen20", 0),
                    // Following split is unclear
                    En1545FixedInteger(EVENT_UNKNOWN_E + "1", 5),
                    En1545FixedInteger(EVENT_UNKNOWN_E + "2", 16),
                    En1545FixedInteger("NeverSeen23", 0),
                    // 8
                    En1545FixedInteger("NeverSeen24", 0),
                    En1545FixedInteger("NeverSeen25", 0),
                    En1545FixedInteger("NeverSeen26", 0),
                    En1545FixedInteger(EVENT_CONTRACT_POINTER, 4),
                    // 1
                    En1545Bitmap( // afc0 or abc0
                        En1545FixedInteger("NeverSeenExtra0", 0),
                        En1545FixedInteger("NeverSeenExtra1", 0),
                        En1545FixedInteger("NeverSeenExtra2", 0),
                        En1545FixedInteger("NeverSeenExtra3", 0),
                        // c
                        En1545FixedInteger("NeverSeenExtra4", 0),
                        En1545FixedInteger("NeverSeenExtra5", 0),
                        En1545FixedInteger.timeLocal(EVENT_FIRST_STAMP),
                        En1545FixedInteger(EVENT_UNKNOWN_G, 1),
                        // f or b
                        En1545FixedInteger(EVENT_FIRST_LOCATION_ID, 11),
                        En1545FixedInteger(EVENT_UNKNOWN_H + "1", 1),
                        En1545FixedInteger(EVENT_UNKNOWN_H + "2", 2),
                        En1545FixedInteger(TRANSACTION_COUNTER, 4),
                        // a
                        En1545FixedInteger("NeverSeenExtra12", 0),
                        En1545Container(
                            En1545FixedInteger(TRAIN_USED_FLAG, 1),
                            En1545FixedInteger(TRAM_USED_FLAG, 1),
                        ),
                        En1545FixedInteger("NeverSeenExtra14", 0),
                        En1545FixedInteger(EVENT_UNKNOWN_I, 1),
                    ),
                ),
                // Rest: 64 bits of 0
            )

        private const val TRANSACTION_TAP_ON = 1
        private const val TRANSACTION_TAP_ON_TRANSFER = 2
        private const val TRANSACTION_TAP_OFF = 3

        fun parse(
            tripData: ByteArray,
            stringResource: StringResource,
        ) = RicaricaMiTransaction(
            En1545Parser.parse(tripData, TRIP_FIELDS),
            stringResource,
        )
    }
}
