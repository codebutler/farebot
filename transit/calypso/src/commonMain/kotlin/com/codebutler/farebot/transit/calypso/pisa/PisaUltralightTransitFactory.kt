/*
 * PisaUltralightTransitFactory.kt
 *
 * Copyright 2018-2019 Google
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

package com.codebutler.farebot.transit.calypso.pisa

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Station
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
import farebot.transit.calypso.generated.resources.*
import kotlinx.datetime.TimeZone

private val NAME by lazy { FormattedString(Res.string.pisa_ultralight_card_name) }

/**
 * Pisa Ultralight transit cards (Pisa, Italy).
 * Ported from Metrodroid's PisaUltralightTransitData.kt.
 */
class PisaUltralightTransitFactory : TransitFactory<UltralightCard, PisaUltralightTransitInfo> {
    override val allCards: List<CardInfo> = emptyList()

    override fun check(card: UltralightCard): Boolean {
        val page4 = card.getPage(4).data
        val networkId = page4.byteArrayToInt(0, 3)
        return networkId == PISA_NETWORK_ID
    }

    override fun parseIdentity(card: UltralightCard): TransitIdentity = TransitIdentity.create(NAME, null)

    override fun parseInfo(card: UltralightCard): PisaUltralightTransitInfo {
        val trips =
            listOf(8, 12).mapNotNull { offset ->
                PisaUltralightTransaction.parse(card.readPages(offset, 4))
            }
        return PisaUltralightTransitInfo(
            mA = card.getPage(4).data[3].toInt() and 0xFF,
            mB = card.readPages(6, 2).byteArrayToLong(),
            trips = TransactionTrip.merge(trips),
        )
    }

    companion object {
        const val PISA_NETWORK_ID = 0x380100
    }
}

class PisaUltralightTransitInfo(
    private val mA: Int,
    private val mB: Long,
    override val trips: List<Trip> = emptyList(),
) : TransitInfo() {
    override val cardName: FormattedString = NAME
    override val serialNumber: String? = null

    override val info: List<ListItemInterface>
        get() =
            listOf(
                ListItem(Res.string.pisa_field_a, mA.toString()),
                ListItem(Res.string.pisa_field_b, mB.toString(16)),
            )
}

private class PisaUltralightTransaction(
    override val parsed: En1545Parsed,
) : En1545Transaction() {
    override val lookup: En1545Lookup = PisaUltralightLookup

    companion object {
        private val TRIP_FIELDS =
            En1545Container(
                En1545FixedInteger.date(En1545Transaction.EVENT),
                En1545FixedInteger.timeLocal(En1545Transaction.EVENT),
                En1545FixedInteger(En1545Transaction.EVENT_UNKNOWN_A, 18),
                En1545FixedInteger("ValueB", 16),
                En1545FixedInteger("ValueA", 16),
                En1545FixedHex(En1545Transaction.EVENT_UNKNOWN_B, 37),
                En1545FixedInteger(En1545Transaction.EVENT_AUTHENTICATOR, 16),
            )

        fun parse(data: ByteArray): PisaUltralightTransaction? {
            val first4 = data.byteArrayToInt(0, 4)
            if (first4 == 0) return null
            return PisaUltralightTransaction(En1545Parser.parse(data, TRIP_FIELDS))
        }
    }
}

private object PisaUltralightLookup : En1545Lookup {
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
    ): FormattedString? = null

    override fun getMode(
        agency: Int?,
        route: Int?,
    ): Trip.Mode = Trip.Mode.OTHER
}
