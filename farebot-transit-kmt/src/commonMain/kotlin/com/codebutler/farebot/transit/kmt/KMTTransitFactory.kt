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

import com.codebutler.farebot.card.felica.FelicaCard
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.card.felica.FeliCaUtil
import farebot.farebot_transit_kmt.generated.resources.Res
import farebot.farebot_transit_kmt.generated.resources.kmt_longname
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class KMTTransitFactory : TransitFactory<FelicaCard, KMTTransitInfo> {

    companion object {
        //Taken from NXP TagInfo reader data

        //This should be in the FeliCaLib from Klazz
        private const val SYSTEMCODE_KMT = 0x90b7

        private const val FELICA_SERVICE_KMT_ID = 0x300B
        private const val FELICA_SERVICE_KMT_BALANCE = 0x1017
        private const val FELICA_SERVICE_KMT_HISTORY = 0x200F
    }

    override fun check(card: FelicaCard): Boolean {
        return card.getSystem(SYSTEMCODE_KMT) != null
    }

    override fun parseIdentity(card: FelicaCard): TransitIdentity {
        val serviceID = card.getSystem(SYSTEMCODE_KMT)!!.getService(FELICA_SERVICE_KMT_ID)
        var serialNumber = "-"
        if (serviceID != null) {
            serialNumber = serviceID.blocks[0].data.decodeToString()
        }

        return TransitIdentity.create(runBlocking { getString(Res.string.kmt_longname) }, serialNumber)
    }

    override fun parseInfo(card: FelicaCard): KMTTransitInfo {
        val serviceID = card.getSystem(SYSTEMCODE_KMT)!!.getService(FELICA_SERVICE_KMT_ID)
        val serialNumber: ByteArray = if (serviceID != null) {
            serviceID.blocks[0].data
        } else {
            "000000000000000".encodeToByteArray()
        }
        val serviceBalance = card.getSystem(SYSTEMCODE_KMT)!!.getService(FELICA_SERVICE_KMT_BALANCE)
        var currentBalance = 0
        if (serviceBalance != null) {
            val blocksBalance = serviceBalance.blocks
            val blockBalance = blocksBalance[0]
            val dataBalance = blockBalance.data
            currentBalance = FeliCaUtil.toInt(dataBalance[3], dataBalance[2], dataBalance[1], dataBalance[0])
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
        return KMTTransitInfo.create(trips, serialNumber, currentBalance)
    }
}
