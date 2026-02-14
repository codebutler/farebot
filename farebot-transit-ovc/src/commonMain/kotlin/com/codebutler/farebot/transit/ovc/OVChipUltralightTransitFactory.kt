/*
 * OVChipUltralightTransitFactory.kt
 *
 * Copyright 2018 Google
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

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545Container
import com.codebutler.farebot.transit.en1545.En1545FixedHex
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Transaction
import kotlinx.datetime.TimeZone

private const val NAME = "OV-chipkaart (single-use)"

/**
 * OV-chipkaart single-use Ultralight cards (Netherlands).
 * Ported from Metrodroid.
 */
class OVChipUltralightTransitFactory : TransitFactory<UltralightCard, OVChipUltralightTransitInfo> {
    override val allCards: List<CardInfo> = emptyList()

    override fun check(card: UltralightCard): Boolean {
        val firstByte = card.getPage(4).data[0]
        return firstByte == 0xc0.toByte() || firstByte == 0xc8.toByte()
    }

    override fun parseIdentity(card: UltralightCard): TransitIdentity = TransitIdentity.create(NAME, null)

    override fun parseInfo(card: UltralightCard): OVChipUltralightTransitInfo {
        val trips =
            listOf(4, 8).mapNotNull { offset ->
                OvcUltralightTransaction.parse(card.readPages(offset, 4))
            }
        return OVChipUltralightTransitInfo(
            trips = TransactionTrip.merge(trips),
        )
    }
}

class OVChipUltralightTransitInfo(
    override val trips: List<Trip> = emptyList(),
) : TransitInfo() {
    override val cardName: String = NAME
    override val serialNumber: String? = null
}

private class OvcUltralightTransaction(
    override val parsed: En1545Parsed,
) : En1545Transaction() {
    override val lookup: En1545Lookup = OvcUltralightLookup

    private val transactionType: Int
        get() = parsed.getIntOrZero(TRANSACTION_TYPE)

    private val company: Int
        get() = parsed.getIntOrZero(EVENT_SERVICE_PROVIDER)

    override val isTapOn: Boolean
        get() = transactionType == PROCESS_CHECKIN

    override val isTapOff: Boolean
        get() = transactionType == PROCESS_CHECKOUT

    override val mode: Trip.Mode
        get() {
            val startStationId = stationId ?: 0
            when (transactionType) {
                PROCESS_BANNED -> return Trip.Mode.BANNED
                PROCESS_CREDIT, PROCESS_PURCHASE, PROCESS_NODATA -> return Trip.Mode.TICKET_MACHINE
            }
            return when (company) {
                AGENCY_NS -> Trip.Mode.TRAIN
                AGENCY_TLS, AGENCY_DUO, AGENCY_STORE -> Trip.Mode.OTHER
                AGENCY_GVB -> if (startStationId < 3000) Trip.Mode.METRO else Trip.Mode.BUS
                AGENCY_RET -> if (startStationId < 3000) Trip.Mode.METRO else Trip.Mode.BUS
                AGENCY_ARRIVA ->
                    when (startStationId) {
                        in 0..800 -> Trip.Mode.TRAIN
                        in 4601..4699 -> Trip.Mode.FERRY
                        else -> Trip.Mode.BUS
                    }
                else -> Trip.Mode.BUS
            }
        }

    override fun isSameTrip(other: Transaction): Boolean {
        if (other !is OvcUltralightTransaction) return false
        if (company != other.company) return false
        val date = parsed.getIntOrZero(En1545FixedInteger.dateName(EVENT))
        val otherDate = other.parsed.getIntOrZero(En1545FixedInteger.dateName(EVENT))
        if (date == otherDate) return true
        if (date != otherDate - 1) return false
        // NS trips reset at 4 AM
        if (company == AGENCY_NS) {
            val otherTime = other.parsed.getIntOrZero(En1545FixedInteger.timeLocalName(EVENT))
            return otherTime < 240
        }
        return true
    }

    companion object {
        private const val TRANSACTION_TYPE = "TransactionType"
        private const val PROCESS_PURCHASE = 0x00
        private const val PROCESS_CHECKIN = 0x01
        private const val PROCESS_CHECKOUT = 0x02
        private const val PROCESS_BANNED = 0x07
        private const val PROCESS_CREDIT = -0x02
        private const val PROCESS_NODATA = -0x03

        private const val AGENCY_TLS = 0x00
        private const val AGENCY_GVB = 0x02
        private const val AGENCY_NS = 0x04
        private const val AGENCY_RET = 0x05
        private const val AGENCY_ARRIVA = 0x08
        private const val AGENCY_DUO = 0x0C
        private const val AGENCY_STORE = 0x19

        private val TRIP_FIELDS =
            En1545Container(
                En1545FixedInteger("A", 8),
                En1545FixedInteger(En1545Transaction.EVENT_SERIAL_NUMBER, 12),
                En1545FixedInteger(En1545Transaction.EVENT_SERVICE_PROVIDER, 12),
                En1545FixedInteger(TRANSACTION_TYPE, 3),
                En1545FixedInteger.date(En1545Transaction.EVENT),
                En1545FixedInteger.timeLocal(En1545Transaction.EVENT),
                En1545FixedInteger("balseqno", 4),
                En1545FixedHex("D", 64),
            )

        fun parse(data: ByteArray): OvcUltralightTransaction? {
            if (data.all { it == 0.toByte() }) return null
            return OvcUltralightTransaction(En1545Parser.parse(data, TRIP_FIELDS))
        }
    }
}

private object OvcUltralightLookup : En1545Lookup {
    override val timeZone: TimeZone = TimeZone.of("Europe/Amsterdam")

    override fun parseCurrency(price: Int) = TransitCurrency(price, "EUR")

    override fun getRouteName(
        routeNumber: Int?,
        routeVariant: Int?,
        agency: Int?,
        transport: Int?,
    ): String? = null

    override fun getAgencyName(
        agency: Int?,
        isShort: Boolean,
    ): String? = null

    override fun getStation(
        station: Int,
        agency: Int?,
        transport: Int?,
    ): Station? {
        if (station == 0 || agency == null) return null
        val companyCodeShort = agency and 0xFFFF
        if (companyCodeShort == 0) return null
        return Station.nameOnly("$companyCodeShort/$station")
    }

    override fun getSubscriptionName(
        stringResource: StringResource,
        agency: Int?,
        contractTariff: Int?,
    ): String? = null

    override fun getMode(
        agency: Int?,
        route: Int?,
    ): Trip.Mode = Trip.Mode.OTHER
}
