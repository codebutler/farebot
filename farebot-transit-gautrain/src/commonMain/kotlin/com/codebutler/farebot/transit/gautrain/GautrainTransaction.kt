/*
 * GautrainTransaction.kt
 *
 * Copyright 2019 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.gautrain

import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Transaction

private const val TRANSACTION_TYPE = "TransactionType"

private fun neverSeenField(i: Int) = En1545FixedInteger("NeverSeen$i", 8)

/**
 * EN1545 trip fields for OVChip-format transactions (reversed bitmap).
 * Matches Metrodroid's OVChipTransaction.tripFields(reversed = true).
 */
internal val GAUTRAIN_TRIP_FIELDS =
    En1545Bitmap.infixBitmap(
        En1545Container(
            En1545FixedInteger.date(En1545Transaction.EVENT),
            En1545FixedInteger.timeLocal(En1545Transaction.EVENT),
        ),
        neverSeenField(1),
        En1545FixedInteger(En1545Transaction.EVENT_UNKNOWN_A, 24),
        En1545FixedInteger(TRANSACTION_TYPE, 7),
        neverSeenField(4),
        En1545FixedInteger(En1545Transaction.EVENT_SERVICE_PROVIDER, 16),
        neverSeenField(6),
        En1545FixedInteger(En1545Transaction.EVENT_SERIAL_NUMBER, 24),
        neverSeenField(8),
        En1545FixedInteger(En1545Transaction.EVENT_LOCATION_ID, 16),
        neverSeenField(10),
        En1545FixedInteger(En1545Transaction.EVENT_DEVICE_ID, 24),
        neverSeenField(12),
        neverSeenField(13),
        neverSeenField(14),
        En1545FixedInteger(En1545Transaction.EVENT_VEHICLE_ID, 16),
        neverSeenField(16),
        En1545FixedInteger(En1545Transaction.EVENT_CONTRACT_POINTER, 5),
        neverSeenField(18),
        neverSeenField(19),
        neverSeenField(20),
        En1545FixedInteger("TripDurationMinutes", 16),
        neverSeenField(22),
        neverSeenField(23),
        En1545FixedInteger(En1545Transaction.EVENT_PRICE_AMOUNT, 16),
        En1545FixedInteger("EventSubscriptionID", 13),
        En1545FixedInteger(En1545Transaction.EVENT_UNKNOWN_C, 10),
        neverSeenField(27),
        En1545FixedInteger("EventExtra", 0),
        reversed = true,
    )

class GautrainTransaction(
    override val parsed: En1545Parsed,
) : En1545Transaction() {
    private val txnType: Int? get() = parsed.getInt(TRANSACTION_TYPE)

    override val isTapOff: Boolean get() = txnType == 0x2a
    override val isTapOn: Boolean get() = txnType == 0x29

    override val lookup: En1545Lookup = GautrainLookup

    override val mode: Trip.Mode
        get() =
            when (txnType) {
                null -> Trip.Mode.TICKET_MACHINE
                0x29, 0x2a -> Trip.Mode.TRAIN
                else -> Trip.Mode.OTHER
            }

    companion object {
        fun parse(raw: ByteArray): GautrainTransaction? {
            if (raw.isAllZero()) return null
            return GautrainTransaction(
                parsed = En1545Parser.parse(raw, GAUTRAIN_TRIP_FIELDS),
            )
        }
    }
}
