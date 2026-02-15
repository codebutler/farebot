/*
 * HafilatTransitFactory.kt
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.hafilat

import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.calypso.IntercodeFields
import com.codebutler.farebot.transit.en1545.En1545Parser
import farebot.transit.hafilat.generated.resources.*
import com.codebutler.farebot.base.util.FormattedString

class HafilatTransitFactory(
) : TransitFactory<DesfireCard, HafilatTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: DesfireCard): Boolean = card.getApplication(APP_ID) != null

    override fun parseIdentity(card: DesfireCard): TransitIdentity =
        TransitIdentity.create(
            FormattedString(Res.string.card_name_hafilat),
            HafilatTransitInfo.formatSerial(getSerial(card.tagId)),
        )

    override fun parseInfo(card: DesfireCard): HafilatTransitInfo {
        val app = card.getApplication(APP_ID)!!

        // 0 = TICKETING_ENVIRONMENT
        val envFile = app.getFile(0) as? StandardDesfireFile
        val parsed =
            if (envFile != null) {
                En1545Parser.parse(envFile.data, IntercodeFields.TICKET_ENV_FIELDS)
            } else {
                null
            }

        val transactionList = mutableListOf<Transaction>()

        // 3-6: TICKETING_LOG
        for (fileId in intArrayOf(3, 4, 5, 6)) {
            val file = app.getFile(fileId) as? StandardDesfireFile ?: continue
            val data = file.data
            if (data.getBitsFromBuffer(0, 14) == 0) continue
            transactionList.add(HafilatTransaction(data))
        }

        val subs = mutableListOf<HafilatSubscription>()
        var purse: HafilatSubscription? = null

        // 10-13: contracts
        for (fileId in intArrayOf(0x10, 0x11, 0x12, 0x13)) {
            val file = app.getFile(fileId) as? StandardDesfireFile ?: continue
            val data = file.data
            if (data.getBitsFromBuffer(0, 7) == 0) continue
            val sub = HafilatSubscription.parse(data)
            if (sub.isPurse) {
                purse = sub
            } else {
                subs.add(sub)
            }
        }

        return HafilatTransitInfo(
            purse = purse,
            serial = getSerial(card.tagId),
            subscriptions = if (subs.isNotEmpty()) subs else null,
            trips = TransactionTrip.merge(transactionList),
        )
    }

    companion object {
        private const val APP_ID = 0x107f2

        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.card_name_hafilat,
                cardType = CardType.MifareDesfire,
                region = TransitRegion.UAE,
                locationRes = Res.string.location_abu_dhabi,
                imageRes = Res.drawable.hafilat,
                latitude = 24.4539f,
                longitude = 54.3773f,
                brandColor = 0x95A966,
                credits = listOf("Metrodroid Project", "Michael Farrell"),
            )

        private fun getSerial(tagId: ByteArray): Long = tagId.byteArrayToLongReversed(1, 6)
    }
}
