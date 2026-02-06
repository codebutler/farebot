/*
 * OVChipTransaction.kt
 *
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2012 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc

import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545Bitmap
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Transaction
import com.codebutler.farebot.transit.en1545.getBitsFromBuffer

class OVChipTransaction(override val parsed: En1545Parsed) : En1545Transaction() {
    private val date: Int
        get() = parsed.getIntOrZero(En1545FixedInteger.dateName(EVENT))

    private val time: Int
        get() = parsed.getIntOrZero(En1545FixedInteger.timeLocalName(EVENT))

    private val transfer: Int
        get() = parsed.getIntOrZero(TRANSACTION_TYPE)

    private val company: Int
        get() = parsed.getIntOrZero(EVENT_SERVICE_PROVIDER)

    val id: Int
        get() = parsed.getIntOrZero(EVENT_SERIAL_NUMBER)

    override val lookup: En1545Lookup get() = OvcLookup

    override val isTapOn get() = transfer == PROCESS_CHECKIN

    override val isTapOff get() = transfer == PROCESS_CHECKOUT

    override fun isSameTrip(other: Transaction): Boolean {
        if (other !is OVChipTransaction)
            return false
        /*
         * Information about checking in and out:
         * http://www.chipinfo.nl/inchecken/
         */

        if (company != other.company)
            return false
        if (date == other.date) {
            return true
        }
        if (date != other.date - 1)
            return false

        // All NS trips get reset at 4 AM (except if it's a night train, but that's out of our scope).
        if (company == AGENCY_NS) {
            return other.time < 240
        }

        /*
         * Some companies expect a checkout at the maximum of 15 minutes after the estimated arrival at the
         * endstation of the line.
         * But it's hard to determine the length of every single trip there is, so for now let's just assume a
         * checkout at the next day is still from the same trip. Better solutions are always welcome ;)
         */
        return true
    }

    override val mode get(): Trip.Mode {
        val startStationId = stationId ?: 0

        // FIXME: Clean this up
        //mIsBusOrTram = (company == AGENCY_GVB || company == AGENCY_HTM || company == AGENCY_RET && (!mIsMetro));
        //mIsBusOrTrain = company == AGENCY_VEOLIA || company == AGENCY_SYNTUS;

        when (transfer) {
            PROCESS_BANNED -> return Trip.Mode.BANNED
            PROCESS_CREDIT -> return Trip.Mode.TICKET_MACHINE
            // Not 100% sure about what NODATA is, but looks alright so far
            PROCESS_PURCHASE, PROCESS_NODATA -> return Trip.Mode.TICKET_MACHINE
        }

        return when (company) {
            AGENCY_NS -> Trip.Mode.TRAIN
            AGENCY_TLS, AGENCY_DUO, AGENCY_STORE -> Trip.Mode.OTHER
            // TODO: Needs verification!
            AGENCY_GVB -> if (startStationId < 3000) Trip.Mode.METRO else Trip.Mode.BUS
            // TODO: Needs verification!
            AGENCY_RET -> if (startStationId < 3000) Trip.Mode.METRO else Trip.Mode.BUS
            AGENCY_ARRIVA -> when (startStationId) {
                in 0..800 -> Trip.Mode.TRAIN
                // TODO: Needs verification!
                in 4601..4699 -> Trip.Mode.FERRY
                else -> Trip.Mode.BUS
            }
            // Everything else will be a bus, although this is not correct.
            // The only way to determine them would be to collect every single 'ovcid' out there :(
            else -> Trip.Mode.BUS
        }
    }

    companion object {
        private const val PROCESS_PURCHASE = 0x00
        private const val PROCESS_CHECKIN = 0x01
        private const val PROCESS_CHECKOUT = 0x02
        private const val PROCESS_TRANSFER = 0x06
        private const val PROCESS_BANNED = 0x07
        private const val PROCESS_CREDIT = -0x02
        private const val PROCESS_NODATA = -0x03

        private const val AGENCY_TLS = 0x00
        private const val AGENCY_GVB = 0x02
        private const val AGENCY_NS = 0x04
        private const val AGENCY_RET = 0x05
        private const val AGENCY_ARRIVA = 0x08
        private const val AGENCY_DUO = 0x0C    // Could also be 2C though... ( http://www.ov-chipkaart.me/forum/viewtopic.php?f=10&t=299 )
        private const val AGENCY_STORE = 0x19
        private const val AGENCY_GVB_FLEX = 0x2711

        const val TRANSACTION_TYPE = "TransactionType"

        private fun neverSeen(i: Int) = "NeverSeen$i"

        private fun neverSeenField(i: Int) = En1545FixedInteger(neverSeen(i), 8)

        fun tripFields(reversed: Boolean = false) = En1545Bitmap.infixBitmap(
                En1545Container(
                        En1545FixedInteger.date(EVENT),
                        En1545FixedInteger.timeLocal(EVENT)
                ),
                neverSeenField(1),
                En1545FixedInteger(EVENT_UNKNOWN_A, 24),
                En1545FixedInteger(TRANSACTION_TYPE, 7),
                neverSeenField(4),
                En1545FixedInteger(EVENT_SERVICE_PROVIDER, 16),
                neverSeenField(6),
                En1545FixedInteger(EVENT_SERIAL_NUMBER, 24),
                neverSeenField(8),
                En1545FixedInteger(EVENT_LOCATION_ID, 16),
                neverSeenField(10),
                En1545FixedInteger(EVENT_DEVICE_ID, 24),
                neverSeenField(12),
                neverSeenField(13),
                neverSeenField(14),
                En1545FixedInteger(EVENT_VEHICLE_ID, 16),
                neverSeenField(16),
                En1545FixedInteger(EVENT_CONTRACT_POINTER, 5),
                neverSeenField(18),
                neverSeenField(19),
                neverSeenField(20),
                En1545FixedInteger("TripDurationMinutes", 16),
                neverSeenField(22),
                neverSeenField(23),
                En1545FixedInteger(EVENT_PRICE_AMOUNT, 16),
                En1545FixedInteger("EventSubscriptionID", 13),
                // Could be from 8 to 10
                En1545FixedInteger(EVENT_UNKNOWN_C, 10),
                neverSeenField(27),
                En1545FixedInteger("EventExtra", 0),
                reversed = reversed
        )

        private val OVC_UL_TRIP_FIELDS = En1545Container(
                En1545FixedInteger("A", 8),
                En1545FixedInteger(EVENT_SERIAL_NUMBER, 12),
                En1545FixedInteger(EVENT_SERVICE_PROVIDER, 12),
                En1545FixedInteger(TRANSACTION_TYPE, 3),
                En1545FixedInteger.date(EVENT),
                En1545FixedInteger.timeLocal(EVENT),
                En1545FixedInteger("balseqno", 4),
                En1545FixedHex("D", 64)
        )

        fun parseClassic(data: ByteArray): OVChipTransaction? {
            if (data.getBitsFromBuffer(0, 28) == 0)
                return null
            val parsed = En1545Parser.parse(data, tripFields())
            // 27 is not critical, ignore if ever
            for (i in 1..23)
                if (parsed.contains(neverSeen(i)))
                    return null
            return OVChipTransaction(parsed)
        }

        fun parseUltralight(data: ByteArray): OVChipTransaction? {
            if (data.isAllZero())
                return null
            return OVChipTransaction(En1545Parser.parse(data, OVC_UL_TRIP_FIELDS))
        }
    }
}
