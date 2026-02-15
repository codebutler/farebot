/*
 * KMTTransitFactory.kt
 *
 * Authors:
 * Bondan Sumbodo <sybond@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.codebutler.farebot.transit.kmt

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.felica.FeliCaUtil
import com.codebutler.farebot.card.felica.FelicaCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import farebot.transit.kmt.generated.resources.*
import com.codebutler.farebot.base.util.FormattedString

class KMTTransitFactory : TransitFactory<FelicaCard, KMTTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    companion object {
        // Taken from NXP TagInfo reader data

        // This should be in the FeliCaLib from Klazz
        private const val SYSTEMCODE_KMT = 0x90b7

        private const val FELICA_SERVICE_KMT_ID = 0x300B
        private const val FELICA_SERVICE_KMT_BALANCE = 0x1017
        private const val FELICA_SERVICE_KMT_HISTORY = 0x200F

        private val CARD_INFO =
            CardInfo(
                nameRes = Res.string.kmt_longname,
                cardType = CardType.FeliCa,
                region = TransitRegion.INDONESIA,
                locationRes = Res.string.location_jakarta_indonesia,
                imageRes = Res.drawable.kmt_card,
                latitude = -6.2088f,
                longitude = 106.8456f,
                brandColor = 0x97D2C4,
                credits = listOf("Bondan Sumbodo"),
                extraNoteRes = Res.string.kmt_notes,
            )
    }

    override fun check(card: FelicaCard): Boolean = card.getSystem(SYSTEMCODE_KMT) != null

    override fun parseIdentity(card: FelicaCard): TransitIdentity {
        val serviceID = card.getSystem(SYSTEMCODE_KMT)!!.getService(FELICA_SERVICE_KMT_ID)
        var serialNumber = "-"
        if (serviceID != null) {
            serialNumber = serviceID.blocks[0].data.decodeToString()
        }

        return TransitIdentity.create(FormattedString(Res.string.kmt_longname), serialNumber)
    }

    override fun parseInfo(card: FelicaCard): KMTTransitInfo {
        val serviceID = card.getSystem(SYSTEMCODE_KMT)!!.getService(FELICA_SERVICE_KMT_ID)
        val serialNumber: ByteArray =
            if (serviceID != null) {
                serviceID.blocks[0].data
            } else {
                "000000000000000".encodeToByteArray()
            }
        val serviceBalance = card.getSystem(SYSTEMCODE_KMT)!!.getService(FELICA_SERVICE_KMT_BALANCE)
        var currentBalance = 0
        var transactionCounter = 0
        var lastTransAmount = 0
        if (serviceBalance != null) {
            val blocksBalance = serviceBalance.blocks
            val blockBalance = blocksBalance[0]
            val dataBalance = blockBalance.data
            currentBalance = FeliCaUtil.toInt(dataBalance[3], dataBalance[2], dataBalance[1], dataBalance[0])
            transactionCounter = FeliCaUtil.toInt(dataBalance[13], dataBalance[14], dataBalance[15])
            lastTransAmount = FeliCaUtil.toInt(dataBalance[7], dataBalance[6], dataBalance[5], dataBalance[4])
        }
        val serviceHistory = card.getSystem(SYSTEMCODE_KMT)!!.getService(FELICA_SERVICE_KMT_HISTORY)!!
        val trips = mutableListOf<Trip>()
        val blocks = serviceHistory.blocks
        for (i in blocks.indices) {
            val block = blocks[i]
            if (block.data[0] != 0.toByte()) {
                val trip = KMTTrip.create(block)
                trips.add(trip)
            }
        }
        return KMTTransitInfo.create(trips, serialNumber, currentBalance, transactionCounter, lastTransAmount)
    }
}
