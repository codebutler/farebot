/*
 * VeneziaUltralightTransitFactory.kt
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

package com.codebutler.farebot.transit.calypso.venezia

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Subscription
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
import com.codebutler.farebot.transit.en1545.En1545Subscription
import com.codebutler.farebot.transit.en1545.En1545Transaction
import com.codebutler.farebot.transit.en1545.getBitsFromBuffer
import farebot.transit.calypso.generated.resources.*
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.getString
import kotlin.time.Instant
import org.jetbrains.compose.resources.StringResource as ComposeStringResource
import com.codebutler.farebot.base.util.FormattedString

private val NAME by lazy { FormattedString(Res.string.venezia_ultralight_card_name) }
private const val TRANSPORT_TYPE = "TransportType"
private const val Y_VALUE = "Y"

/**
 * Venezia Ultralight transit cards (Venice, Italy).
 * Ported from Metrodroid's VeneziaUltralightTransitData.kt.
 */
class VeneziaUltralightTransitFactory : TransitFactory<UltralightCard, VeneziaUltralightTransitInfo> {
    override val allCards: List<CardInfo> = emptyList()

    override fun check(card: UltralightCard): Boolean {
        val otp = card.getPage(3).data
        val otpVal = otp.byteArrayToInt(0, 2)
        return otpVal in VENEZIA_OTP_VALUES
    }

    override fun parseIdentity(card: UltralightCard): TransitIdentity =
        TransitIdentity.create(NAME, getSerial(card).toString())

    override fun parseInfo(card: UltralightCard): VeneziaUltralightTransitInfo {
        val head = card.readPages(4, 4)
        val tripFormat = head.getBitsFromBuffer(32, 4)

        val transactions =
            listOf(
                card.readPages(8, 4),
                card.readPages(12, 4),
            ).mapNotNull { VeneziaUltralightTransaction.parse(it, tripFormat) }

        val lastTransaction = transactions.maxByOrNull { it.timestamp?.toEpochMilliseconds() ?: 0L }

        val otp = card.getPage(3).data
        val otpVal = otp.byteArrayToInt(2, 2)

        val sub =
            VeneziaUltralightSubscription(
                parsed = En1545Parser.parse(head, SUBSCRIPTION_FIELDS),
                validToOverride = lastTransaction?.expiryTimestamp,
                otp = otpVal,
            )

        val trips = TransactionTrip.merge(transactions)

        return VeneziaUltralightTransitInfo(
            serial = getSerial(card),
            trips = trips,
            subscriptions = listOfNotNull(sub),
        )
    }

    companion object {
        private val VENEZIA_OTP_VALUES = listOf(0x30de, 0x3186, 0x4ca8, 0x6221)

        private val SUBSCRIPTION_FIELDS =
            En1545Container(
                En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_A, 32),
                En1545FixedInteger(TRIP_FORMAT, 4),
                En1545FixedInteger(En1545Subscription.CONTRACT_TARIFF, 16),
                En1545FixedHex(En1545Subscription.CONTRACT_UNKNOWN_B, 44),
                En1545FixedInteger(En1545Subscription.CONTRACT_AUTHENTICATOR, 32),
            )

        private const val TRIP_FORMAT = "TripFormat"

        private fun getSerial(card: UltralightCard): Long {
            val page0 = card.getPage(0).data
            val page1 = card.getPage(1).data
            val bytes = ByteArray(7)
            page0.copyInto(bytes, 0, 0, 3)
            page1.copyInto(bytes, 3, 0, 4)
            return bytes.byteArrayToLongReversed()
        }
    }
}

class VeneziaUltralightTransitInfo internal constructor(
    private val serial: Long,
    override val trips: List<Trip>,
    override val subscriptions: List<Subscription>?,
) : TransitInfo() {
    override val cardName: FormattedString = NAME
    override val serialNumber: String = serial.toString()
}

internal class VeneziaUltralightSubscription(
    override val parsed: En1545Parsed,
    private val validToOverride: Instant?,
    private val otp: Int,
) : En1545Subscription() {
    override val lookup: En1545Lookup = VeneziaUltralightLookup

    override val validTo: Instant? get() = validToOverride

    override val subscriptionState: SubscriptionState
        get() = if (otp == 0) SubscriptionState.INACTIVE else SubscriptionState.STARTED
}

internal class VeneziaUltralightTransaction(
    override val parsed: En1545Parsed,
) : En1545Transaction() {
    override val lookup: En1545Lookup = VeneziaUltralightLookup

    override val mode: Trip.Mode
        get() {
            when (parsed.getInt(TRANSPORT_TYPE)) {
                1 -> return Trip.Mode.BUS
                5 -> return Trip.Mode.FERRY
            }
            if (parsed.getInt(Y_VALUE) == 1000) {
                return Trip.Mode.FERRY
            }
            return Trip.Mode.BUS
        }

    val expiryTimestamp: Instant?
        get() = parsed.getTimeStamp(En1545Subscription.CONTRACT_END, VeneziaUltralightLookup.timeZone)

    companion object {
        private val TRIP_FIELDS_FORMAT_1 =
            En1545Container(
                En1545FixedInteger("A", 11),
                En1545FixedInteger.timePacked11Local(En1545Transaction.EVENT),
                En1545FixedInteger(Y_VALUE, 14),
                En1545FixedInteger("B", 2),
                En1545FixedInteger.datePacked(En1545Transaction.EVENT),
                En1545FixedInteger.timePacked11Local(En1545Transaction.EVENT_FIRST_STAMP),
                En1545FixedInteger("Z", 16),
                En1545FixedInteger("C", 17),
                En1545FixedInteger(En1545Transaction.EVENT_AUTHENTICATOR, 32),
            )

        private val TRIP_FIELDS_DEFAULT =
            En1545Container(
                En1545FixedInteger("A", 8),
                En1545FixedInteger.timePacked11Local(En1545Transaction.EVENT),
                En1545FixedInteger(Y_VALUE, 14),
                En1545FixedInteger("B", 2),
                En1545FixedInteger.datePacked(En1545Transaction.EVENT),
                En1545FixedInteger.timePacked11Local(En1545Transaction.EVENT_FIRST_STAMP),
                En1545FixedInteger("D", 2),
                En1545FixedInteger.datePacked(En1545Subscription.CONTRACT_END),
                En1545FixedInteger.timePacked11Local(En1545Subscription.CONTRACT_END),
                En1545FixedInteger(TRANSPORT_TYPE, 4),
                En1545FixedInteger("F", 5),
                En1545FixedInteger(En1545Transaction.EVENT_AUTHENTICATOR, 32),
            )

        fun parse(
            data: ByteArray,
            tripFormat: Int,
        ): VeneziaUltralightTransaction? {
            // Match Metrodroid: check if bytes 1..11 (11 bytes) are all zero
            if (data.sliceOffLen(1, 11).isAllZero()) return null
            val fields = if (tripFormat == 1) TRIP_FIELDS_FORMAT_1 else TRIP_FIELDS_DEFAULT
            return VeneziaUltralightTransaction(En1545Parser.parse(data, fields))
        }
    }
}

private object VeneziaUltralightLookup : En1545Lookup {
    override val timeZone: TimeZone = TimeZone.of("Europe/Rome")

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
    ): FormattedString? = null

    override fun getStation(
        station: Int,
        agency: Int?,
        transport: Int?,
    ): Station? = null

    override fun getSubscriptionName(
        agency: Int?,
        contractTariff: Int?,
    ): FormattedString? {
        if (contractTariff == null) return null
        val res = SUBSCRIPTION_MAP[contractTariff]
        return if (res != null) {
            FormattedString(res)
        } else {
            FormattedString(Res.string.venezia_ul_unknown_subscription, contractTariff.toString())
        }
    }

    override fun getMode(
        agency: Int?,
        route: Int?,
    ): Trip.Mode = Trip.Mode.OTHER

    private val SUBSCRIPTION_MAP: Map<Int, ComposeStringResource> =
        mapOf(
            11105 to Res.string.venezia_ul_24h_ticket,
            11209 to Res.string.venezia_ul_rete_unica_75min,
            11210 to Res.string.venezia_ul_rete_unica_100min,
            12101 to Res.string.venezia_ul_bus_ticket_75min,
            12106 to Res.string.venezia_ul_airport_bus_ticket,
            11400 to Res.string.venezia_ul_carnet_traghetto,
        )
}
