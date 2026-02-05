/*
 * MobibTransaction.kt
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

package com.codebutler.farebot.transit.calypso.mobib

import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Transaction
import com.codebutler.farebot.transit.en1545.getBitsFromBuffer

internal class MobibTransaction private constructor(
    override val parsed: En1545Parsed,
    override val lookup: En1545Lookup
) : En1545Transaction() {

    val transactionNumber: Int = parsed.getIntOrZero(En1545Transaction.EVENT_SERIAL_NUMBER)

    companion object {
        private const val EVENT_VERSION = "EventVersion"
        private const val EVENT_LOCATION_ID_BUS = "EventLocationIdBus"
        private const val EVENT_TRANSFER_NUMBER = "EventTransferNumber"
        private const val EVENT_UNKNOWN_B = "EventUnknownB"
        private const val EVENT_UNKNOWN_C = "EventUnknownC"
        private const val EVENT_UNKNOWN_E = "EventUnknownE"
        private const val EVENT_UNKNOWN_F = "EventUnknownF"
        private const val EVENT_UNKNOWN_G = "EventUnknownG"
        private const val NEVER_SEEN_2 = "NeverSeen2"
        private const val NEVER_SEEN_3 = "NeverSeen3"
        private const val NEVER_SEEN_A3 = "NeverSeenA3"
        private const val NEVER_SEEN_A5 = "NeverSeenA5"

        fun parse(data: ByteArray): MobibTransaction? {
            if (data.all { it == 0.toByte() }) return null

            val version = data.getBitsFromBuffer(0, 6)
            val fields = if (version <= 2) {
                En1545Container(
                    En1545FixedInteger(EVENT_VERSION, 6),
                    En1545FixedInteger.date(En1545Transaction.EVENT),
                    En1545FixedInteger.timeLocal(En1545Transaction.EVENT),
                    En1545FixedInteger(EVENT_UNKNOWN_B, 21),
                    En1545FixedInteger(En1545Transaction.EVENT_PASSENGER_COUNT, 5),
                    En1545FixedInteger(EVENT_UNKNOWN_C, 14),
                    En1545FixedInteger(EVENT_LOCATION_ID_BUS, 12),
                    En1545FixedInteger(En1545Transaction.EVENT_ROUTE_NUMBER, 16),
                    En1545FixedInteger(En1545Transaction.EVENT_SERVICE_PROVIDER, 5),
                    En1545FixedInteger(En1545Transaction.EVENT_LOCATION_ID, 17),
                    En1545FixedInteger(EVENT_UNKNOWN_E, 10),
                    En1545FixedInteger(EVENT_UNKNOWN_F, 7),
                    En1545FixedInteger(En1545Transaction.EVENT_SERIAL_NUMBER, 24),
                    En1545FixedInteger(EVENT_TRANSFER_NUMBER, 24),
                    En1545FixedInteger.date(En1545Transaction.EVENT_FIRST_STAMP),
                    En1545FixedInteger.timeLocal(En1545Transaction.EVENT_FIRST_STAMP),
                    En1545FixedInteger(EVENT_UNKNOWN_G, 21)
                )
            } else {
                En1545Container(
                    En1545FixedInteger(EVENT_VERSION, 6),
                    En1545FixedInteger.date(En1545Transaction.EVENT),
                    En1545FixedInteger.timeLocal(En1545Transaction.EVENT),
                    En1545FixedInteger(EVENT_UNKNOWN_B + "1", 31),
                    En1545Bitmap(
                        En1545Container(
                            En1545FixedInteger(EVENT_UNKNOWN_B + "2", 4),
                            En1545FixedInteger(EVENT_LOCATION_ID_BUS, 12)
                        ),
                        En1545FixedInteger(En1545Transaction.EVENT_ROUTE_NUMBER, 16),
                        En1545FixedInteger(NEVER_SEEN_2, 16),
                        En1545FixedInteger(NEVER_SEEN_3, 16),
                        En1545Container(
                            En1545FixedInteger(En1545Transaction.EVENT_SERVICE_PROVIDER, 5),
                            En1545FixedInteger(En1545Transaction.EVENT_LOCATION_ID, 17),
                            En1545FixedInteger(EVENT_UNKNOWN_E + "1", 10)
                        )
                    ),
                    En1545Bitmap(
                        En1545FixedInteger(En1545Transaction.EVENT_SERIAL_NUMBER, 24),
                        En1545FixedInteger(EVENT_UNKNOWN_F, 16),
                        En1545FixedInteger(EVENT_TRANSFER_NUMBER, 8),
                        En1545FixedInteger(NEVER_SEEN_A3, 16),
                        En1545Container(
                            En1545FixedInteger.date(En1545Transaction.EVENT_FIRST_STAMP),
                            En1545FixedInteger.timeLocal(En1545Transaction.EVENT_FIRST_STAMP)
                        ),
                        En1545FixedInteger(NEVER_SEEN_A5, 16)
                    ),
                    En1545FixedInteger(EVENT_UNKNOWN_G, 21)
                )
            }

            val parsed = En1545Parser.parse(data, fields)
            return MobibTransaction(parsed, MobibLookup)
        }
    }
}
