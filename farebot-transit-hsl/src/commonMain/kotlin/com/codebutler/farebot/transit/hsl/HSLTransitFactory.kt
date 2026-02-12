/*
 * HSLTransitFactory.kt
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.hsl

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.RecordDesfireFile
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_hsl.generated.resources.*

class HSLTransitFactory(
    private val stringResource: StringResource
) : TransitFactory<DesfireCard, HSLTransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: DesfireCard): Boolean {
        return ALL_IDS.any { card.getApplication(it) != null }
    }

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        val dataHSL = card.getApplication(APP_ID_V1)?.getFile(0x08)?.let { it as? StandardDesfireFile }?.data
            ?: card.getApplication(APP_ID_V2)?.getFile(0x08)?.let { it as? StandardDesfireFile }?.data
        if (dataHSL != null) {
            return TransitIdentity.create(CARD_NAME_HSL, formatSerial(dataHSL.hex().substring(2, 20)))
        }
        val dataWaltti = card.getApplication(APP_ID_WALTTI)?.getFile(0x08)?.let { it as? StandardDesfireFile }?.data
        if (dataWaltti != null) {
            return TransitIdentity.create(CARD_NAME_WALTTI, formatSerial(dataWaltti.hex().substring(2, 20)))
        }
        return TransitIdentity.create(CARD_NAME_HSL, null)
    }

    override fun parseInfo(card: DesfireCard): HSLTransitInfo {
        return card.getApplication(APP_ID_V1)?.let { parse(it, HSLVariant.HSL_V1) }
            ?: card.getApplication(APP_ID_V2)?.let { parse(it, HSLVariant.HSL_V2) }
            ?: card.getApplication(APP_ID_WALTTI)?.let { parse(it, HSLVariant.WALTTI) }
            ?: throw RuntimeException("No HSL/Waltti application found")
    }

    companion object {
        private const val CARD_NAME_HSL = "HSL"
        private const val CARD_NAME_WALTTI = "Waltti"

        private val CARD_INFO = CardInfo(
            nameRes = Res.string.hsl_card_name,
            cardType = CardType.MifareDesfire,
            region = TransitRegion.FINLAND,
            locationRes = Res.string.hsl_location,
            imageRes = Res.drawable.hsl_card,
            latitude = 60.1699f,
            longitude = 24.9384f,
            brandColor = 0xB7EC13,
            credits = listOf("Lauri Andler"),
            sampleDumpFile = "HSL.json",
        )

        private const val APP_ID_V1 = 0x1120ef
        internal const val APP_ID_V2 = 0x1420ef
        private const val APP_ID_WALTTI = 0x10ab
        private val HSL_IDS = listOf(APP_ID_V1, APP_ID_V2)
        private val ALL_IDS = HSL_IDS + listOf(APP_ID_WALTTI)

        fun formatSerial(input: String) = NumberUtils.groupString(input, " ", 6, 4, 4)

        private fun parseTrips(
            app: com.codebutler.farebot.card.desfire.DesfireApplication,
            version: HSLVariant
        ): List<HSLTransaction> {
            val recordFile = app.getFile(0x04) as? RecordDesfireFile ?: return emptyList()
            return recordFile.records.mapNotNull { HSLTransaction.parseLog(it.data, version) }
        }

        private fun addEmbedTransaction(trips: MutableList<HSLTransaction>, embed: HSLTransaction) {
            val sameIdx = trips.indices.find { idx -> trips[idx].timestamp == embed.timestamp }
            if (sameIdx != null) {
                val same = trips[sameIdx]
                trips.removeAt(sameIdx)
                trips.add(HSLTransaction.merge(same, embed))
            } else {
                trips.add(embed)
            }
        }

        private fun parse(
            app: com.codebutler.farebot.card.desfire.DesfireApplication,
            version: HSLVariant
        ): HSLTransitInfo {
            val appInfo = (app.getFile(0x08) as? StandardDesfireFile)?.data
            val serialNumber = appInfo?.hex()?.let {
                if (it.length >= 20) formatSerial(it.substring(2, 20)) else null
            }

            val balData = (app.getFile(0x02) as? StandardDesfireFile)?.data
            val mBalance = balData?.getBitsFromBuffer(0, 20) ?: 0
            val mLastRefill = balData?.let { HSLRefill.parse(it) }

            val trips = parseTrips(app, version).toMutableList()

            val arvoData = (app.getFile(0x03) as? StandardDesfireFile)?.data
            val arvo = arvoData?.let { HSLArvo.parse(it, version) }

            val kausiData = (app.getFile(0x01) as? StandardDesfireFile)?.data
            val kausi = kausiData?.let { HSLKausi.parse(it, version) }

            arvo?.lastTransaction?.let { addEmbedTransaction(trips, it) }
            kausi?.transaction?.let { addEmbedTransaction(trips, it) }

            val cardName = if (version == HSLVariant.WALTTI) CARD_NAME_WALTTI else CARD_NAME_HSL

            return HSLTransitInfo(
                serialNumber = serialNumber,
                subscriptions = kausi?.subs.orEmpty() + listOfNotNull(arvo),
                mBalance = mBalance,
                applicationVersion = appInfo?.getBitsFromBuffer(0, 4),
                applicationKeyVersion = appInfo?.getBitsFromBuffer(4, 4),
                platformType = appInfo?.getBitsFromBuffer(80, 3),
                securityLevel = appInfo?.getBitsFromBuffer(83, 1),
                trips = TransactionTrip.merge(trips + listOfNotNull(mLastRefill)),
                cardNameOverride = cardName
            )
        }
    }
}
