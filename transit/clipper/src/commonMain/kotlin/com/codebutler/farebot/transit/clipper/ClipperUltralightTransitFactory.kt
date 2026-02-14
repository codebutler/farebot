/*
 * ClipperUltralightTransitFactory.kt
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

package com.codebutler.farebot.transit.clipper

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import farebot.transit.clipper.generated.resources.Res
import farebot.transit.clipper.generated.resources.clipper_ticket_type
import farebot.transit.clipper.generated.resources.clipper_ticket_type_adult
import farebot.transit.clipper.generated.resources.clipper_ticket_type_rtc
import farebot.transit.clipper.generated.resources.clipper_ticket_type_senior
import farebot.transit.clipper.generated.resources.clipper_ticket_type_youth
import farebot.transit.clipper.generated.resources.clipper_ul_card_name

class ClipperUltralightTransitFactory : TransitFactory<UltralightCard, ClipperUltralightTransitInfo> {
    override val allCards: List<CardInfo> = emptyList()

    override fun check(card: UltralightCard): Boolean = card.getPage(4).data[0].toInt() == 0x13

    override fun parseIdentity(card: UltralightCard): TransitIdentity {
        val cardName = getStringBlocking(Res.string.clipper_ul_card_name)
        return TransitIdentity.create(cardName, getSerial(card).toString())
    }

    override fun parseInfo(card: UltralightCard): ClipperUltralightTransitInfo {
        val page1 = card.getPage(5).data
        val baseDate = byteArrayToInt(page1, 2, 2)

        val rawTrips =
            listOf(6, 11)
                .map { offset ->
                    card.readPages(offset, 5)
                }.filter { !isAllZero(it) }
                .map { ClipperUltralightTrip(it, baseDate) }

        var trLast: ClipperUltralightTrip? = null
        for (tr in rawTrips) {
            if (trLast == null || tr.isSeqGreater(trLast)) {
                trLast = tr
            }
        }

        val subscription =
            ClipperUltralightSubscription(
                product = byteArrayToInt(page1, 0, 2),
                tripsRemaining = trLast?.tripsRemaining ?: -1,
                transferExpiry = trLast?.transferExpiryTime ?: 0,
                baseDate = baseDate,
            )

        val type = card.getPage(4).data[1].toInt() and 0xff

        return ClipperUltralightTransitInfo(
            serial = getSerial(card),
            trips = rawTrips.filter { !it.isHidden },
            subscription = subscription,
            ticketType = type,
            baseDate = baseDate,
        )
    }

    private fun getSerial(card: UltralightCard): Long {
        val otp = card.getPage(3).data
        return byteArrayToLong(otp, 0, 4)
    }

    private fun byteArrayToInt(
        data: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        var result = 0
        for (i in 0 until length) {
            result = result shl 8
            result = result or (data[offset + i].toInt() and 0xFF)
        }
        return result
    }

    private fun byteArrayToLong(
        data: ByteArray,
        offset: Int,
        length: Int,
    ): Long {
        var result = 0L
        for (i in 0 until length) {
            result = result shl 8
            result = result or (data[offset + i].toLong() and 0xFF)
        }
        return result
    }

    private fun isAllZero(data: ByteArray): Boolean = data.all { it == 0.toByte() }
}

class ClipperUltralightTransitInfo(
    private val serial: Long,
    override val trips: List<ClipperUltralightTrip>,
    private val subscription: ClipperUltralightSubscription,
    private val ticketType: Int,
    private val baseDate: Int,
) : TransitInfo() {
    override val cardName: String
        get() = getStringBlocking(Res.string.clipper_ul_card_name)

    override val serialNumber: String = serial.toString()

    override val subscriptions: List<Subscription> = listOf(subscription)

    override val info: List<ListItemInterface>
        get() =
            listOf(
                when (ticketType) {
                    0x04 -> ListItem(Res.string.clipper_ticket_type, Res.string.clipper_ticket_type_adult)
                    0x44 -> ListItem(Res.string.clipper_ticket_type, Res.string.clipper_ticket_type_senior)
                    0x84 -> ListItem(Res.string.clipper_ticket_type, Res.string.clipper_ticket_type_rtc)
                    0xc4 -> ListItem(Res.string.clipper_ticket_type, Res.string.clipper_ticket_type_youth)
                    else -> ListItem(Res.string.clipper_ticket_type, ticketType.toString(16))
                },
            )
}
