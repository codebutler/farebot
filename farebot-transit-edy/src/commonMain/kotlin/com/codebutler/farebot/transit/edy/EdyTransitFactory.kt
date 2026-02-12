/*
 * EdyTransitFactory.kt
 *
 * Authors:
 * Chris Norden
 * Eric Butler <eric@codebutler.com>
 *
 * Based on code from http://code.google.com/p/nfc-felica/
 * nfc-felica by Kazzz. See project URL for complete author information.
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

package com.codebutler.farebot.transit.edy

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.felica.FelicaCard
import com.codebutler.farebot.card.felica.FeliCaConstants
import com.codebutler.farebot.card.felica.FeliCaUtil
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_edy.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class EdyTransitFactory(private val stringResource: StringResource) : TransitFactory<FelicaCard, EdyTransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    companion object {
        private const val FELICA_SERVICE_EDY_ID = 0x110B
        private const val FELICA_SERVICE_EDY_BALANCE = 0x1317
        private const val FELICA_SERVICE_EDY_HISTORY = 0x170F

        private val CARD_INFO = CardInfo(
            nameRes = Res.string.card_name_edy,
            cardType = CardType.FeliCa,
            region = TransitRegion.JAPAN,
            locationRes = Res.string.location_tokyo_japan,
            imageRes = Res.drawable.edy_card,
            latitude = 35.6762f,
            longitude = 139.6503f,
            brandColor = 0x000059,
        )
    }

    override fun check(card: FelicaCard): Boolean {
        return card.getSystem(FeliCaConstants.SYSTEMCODE_EDY) != null
    }

    override fun parseIdentity(card: FelicaCard): TransitIdentity {
        return TransitIdentity.create(runBlocking { getString(Res.string.card_name_edy) }, null)
    }

    override fun parseInfo(card: FelicaCard): EdyTransitInfo {
        // card ID is in block 0, bytes 2-9, big-endian ordering
        val serialNumber = ByteArray(8)
        val serviceID = card.getSystem(FeliCaConstants.SYSTEMCODE_EDY)!!.getService(FELICA_SERVICE_EDY_ID)!!
        val blocksID = serviceID.blocks
        val blockID = blocksID[0]
        val dataID = blockID.data
        for (i in 2 until 10) {
            serialNumber[i - 2] = dataID[i]
        }

        // current balance info in block 0, bytes 0-3, little-endian ordering
        val serviceBalance = card.getSystem(FeliCaConstants.SYSTEMCODE_EDY)!!.getService(FELICA_SERVICE_EDY_BALANCE)!!
        val blocksBalance = serviceBalance.blocks
        val blockBalance = blocksBalance[0]
        val dataBalance = blockBalance.data
        val currentBalance = FeliCaUtil.toInt(dataBalance[3], dataBalance[2], dataBalance[1], dataBalance[0])

        // now read the transaction history
        val serviceHistory = card.getSystem(FeliCaConstants.SYSTEMCODE_EDY)!!.getService(FELICA_SERVICE_EDY_HISTORY)!!
        val trips = mutableListOf<Trip>()

        // Read blocks in order
        val blocks = serviceHistory.blocks
        for (i in blocks.indices) {
            val block = blocks[i]
            val trip = EdyTrip.create(block, stringResource)
            trips.add(trip)
        }

        return EdyTransitInfo.create(trips, serialNumber, currentBalance)
    }
}
