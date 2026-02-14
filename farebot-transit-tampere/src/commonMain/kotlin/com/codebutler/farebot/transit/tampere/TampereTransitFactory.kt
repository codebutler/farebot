/*
 * TampereTransitFactory.kt
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

package com.codebutler.farebot.transit.tampere

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.base.util.readASCII
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.RecordDesfireFile
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_tampere.generated.resources.*

class TampereTransitFactory : TransitFactory<DesfireCard, TampereTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: DesfireCard): Boolean = card.getApplication(TampereTransitInfo.APP_ID) != null

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        val serialNumber = getSerialNumber(card)
        return TransitIdentity.create(getStringBlocking(Res.string.tampere_card_name), serialNumber)
    }

    override fun parseInfo(card: DesfireCard): TampereTransitInfo {
        val app = card.getApplication(TampereTransitInfo.APP_ID)

        val file4Data = (app?.getFile(0x04) as? StandardDesfireFile)?.data
        val holderName = file4Data?.sliceOffLen(6, 24)?.readASCII()
        val holderBirthDate = file4Data?.byteArrayToIntReversed(0x22, 2)
        val issueDate = file4Data?.byteArrayToIntReversed(0x2a, 2)

        val file2Data = (app?.getFile(0x02) as? StandardDesfireFile)?.data
        var balance: Int? = null
        val subs = mutableListOf<TampereSubscription>()

        if (file2Data != null && file2Data.size >= 96) {
            val blockPtr =
                if ((file2Data.byteArrayToInt(0, 1) - file2Data.byteArrayToInt(48, 1)) and 0xff > 0x80) {
                    48
                } else {
                    0
                }
            for (i in 0..2) {
                val contractRaw = file2Data.sliceOffLen(blockPtr + 4 + 12 * i, 12)
                val type = contractRaw.byteArrayToInt(2, 1)
                when (type) {
                    0 -> continue
                    0x3 ->
                        subs +=
                            TampereSubscription(
                                mStart = null,
                                mEnd = contractRaw.byteArrayToInt(6, 2),
                                mType = type,
                            )
                    7 -> balance = contractRaw.byteArrayToInt(7, 2)
                    0xf ->
                        subs +=
                            TampereSubscription(
                                mStart = contractRaw.byteArrayToInt(6, 2),
                                mEnd = contractRaw.byteArrayToInt(8, 2),
                                mType = type,
                            )
                    else -> subs += TampereSubscription(mType = type)
                }
            }
        }

        val trips =
            (app?.getFile(0x03) as? RecordDesfireFile)?.records?.map { record ->
                TampereTrip.parse(record.data)
            }

        val fallbackBalance = (app?.getFile(0x01) as? StandardDesfireFile)?.data?.byteArrayToIntReversed()

        return TampereTransitInfo(
            serialNumber = getSerialNumber(card),
            mBalance = balance ?: fallbackBalance,
            trips = trips,
            mHolderName = holderName,
            mHolderBirthDate = holderBirthDate,
            mIssueDate = issueDate,
            subscriptions = subs.ifEmpty { null },
        )
    }

    private fun getSerialNumber(card: DesfireCard): String? {
        val app = card.getApplication(TampereTransitInfo.APP_ID) ?: return null
        val file7Data = (app.getFile(0x07) as? StandardDesfireFile)?.data ?: return null
        val hexStr = file7Data.hex()
        if (hexStr.length < 20) return null
        val raw = hexStr.substring(2, 20)
        return NumberUtils.groupString(raw, " ", 6, 4, 4, 3)
    }

    companion object {
        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.tampere_card_name,
                cardType = CardType.MifareDesfire,
                region = TransitRegion.FINLAND,
                locationRes = Res.string.tampere_location,
                imageRes = Res.drawable.tampere,
                latitude = 61.4978f,
                longitude = 23.7610f,
                brandColor = 0xA8C9E6,
                credits = listOf("Metrodroid Project"),
            )
    }
}
