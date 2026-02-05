/*
 * BeijingTransitInfo.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/BeijingMunicipal.java
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

package com.codebutler.farebot.transit.china

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.card.china.ChinaCard
import com.codebutler.farebot.card.china.ChinaCardTransitFactory
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_china.generated.resources.Res
import farebot.farebot_transit_china.generated.resources.card_name_beijing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString

/**
 * Transit info implementation for Beijing Municipal Card (BMAC / 北京市政交通一卡通).
 *
 * This is the primary transit card used in Beijing, China. It can be used on:
 * - Beijing Subway
 * - Beijing buses
 * - Taxis (some)
 * - Retail stores (some)
 */
@Serializable
class BeijingTransitInfo(
    val validityStart: Int?,
    val validityEnd: Int?,
    override val serialNumber: String?,
    override val trips: List<ChinaTrip>?,
    val mBalance: Int?
) : TransitInfo() {

    override val cardName: String
        get() = runBlocking { getString(Res.string.card_name_beijing) }

    override val balance: TransitBalance?
        get() = if (mBalance != null)
            TransitBalance(
                balance = TransitCurrency.CNY(mBalance),
                validFrom = ChinaTransitData.parseHexDate(validityStart),
                validTo = ChinaTransitData.parseHexDate(validityEnd)
            )
        else
            null

    companion object {
        private const val FILE_INFO = 0x4

        private fun parse(card: ChinaCard): BeijingTransitInfo {
            val info = ChinaTransitData.getFile(card, FILE_INFO)?.binaryData

            return BeijingTransitInfo(
                serialNumber = parseSerial(card),
                validityStart = info?.byteArrayToInt(0x18, 4),
                validityEnd = info?.byteArrayToInt(0x1c, 4),
                trips = ChinaTransitData.parseTrips(card) { ChinaTrip(it) },
                mBalance = ChinaTransitData.parseBalance(card)
            )
        }

        val FACTORY: ChinaCardTransitFactory = object : ChinaCardTransitFactory {
            override val appNames: List<ByteArray>
                get() = listOf(
                    "OC".encodeToByteArray(),
                    "PBOC".encodeToByteArray()
                )

            override fun parseTransitIdentity(card: ChinaCard): TransitIdentity =
                TransitIdentity(
                    runBlocking { getString(Res.string.card_name_beijing) },
                    parseSerial(card)
                )

            override fun parseTransitData(card: ChinaCard): TransitInfo = parse(card)
        }

        private fun parseSerial(card: ChinaCard): String? =
            ChinaTransitData.getFile(card, FILE_INFO)?.binaryData?.getHexString(0, 8)
    }
}
