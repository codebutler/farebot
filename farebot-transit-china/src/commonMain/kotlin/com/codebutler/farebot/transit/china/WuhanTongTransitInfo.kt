/*
 * WuhanTongTransitInfo.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/WuhanTong.java
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
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.china.ChinaCard
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.china.ChinaCardTransitFactory
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_china.generated.resources.Res
import farebot.farebot_transit_china.generated.resources.card_location_wuhan_china
import farebot.farebot_transit_china.generated.resources.card_name_wuhan_tong
import farebot.farebot_transit_china.generated.resources.card_name_wuhantong
import farebot.farebot_transit_china.generated.resources.wuhantong
import kotlinx.serialization.Serializable

/**
 * Transit info implementation for Wuhan Tong (武汉通) cards.
 *
 * Wuhan Tong is the primary transit card used in Wuhan, China. It can be used on:
 * - Wuhan Metro
 * - Wuhan buses
 * - Wuhan ferries
 * - Retail stores (some)
 */
@Serializable
class WuhanTongTransitInfo(
    val validityStart: Int?,
    val validityEnd: Int?,
    override val serialNumber: String?,
    override val trips: List<ChinaTrip>?,
    val mBalance: Int?
) : TransitInfo() {

    override val cardName: String
        get() = getStringBlocking(Res.string.card_name_wuhantong)

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
        private fun parse(card: ChinaCard): WuhanTongTransitInfo {
            val file5 = ChinaTransitData.getFile(card, 0x5)?.binaryData
            return WuhanTongTransitInfo(
                serialNumber = parseSerial(card),
                validityStart = file5?.byteArrayToInt(20, 4),
                validityEnd = file5?.byteArrayToInt(16, 4),
                trips = ChinaTransitData.parseTrips(card) { ChinaTrip(it) },
                mBalance = ChinaTransitData.parseBalance(card)
            )
        }

        val FACTORY: ChinaCardTransitFactory = object : ChinaCardTransitFactory {
            override val allCards: List<CardInfo> = listOf(
                CardInfo(nameRes = Res.string.card_name_wuhan_tong, cardType = CardType.ISO7816, region = TransitRegion.CHINA, locationRes = Res.string.card_location_wuhan_china, imageRes = Res.drawable.wuhantong, latitude = 30.5928f, longitude = 114.3055f, brandColor = 0x0C2C58, credits = listOf("Metrodroid Project", "Vladimir Serbinenko", "Sinpo Lib"))
            )

            override val appNames: List<ByteArray>
                get() = listOf("AP1.WHCTC".encodeToByteArray())

            override fun parseTransitIdentity(card: ChinaCard): TransitIdentity =
                TransitIdentity(
                    getStringBlocking(Res.string.card_name_wuhantong),
                    parseSerial(card)
                )

            override fun parseTransitData(card: ChinaCard): TransitInfo = parse(card)
        }

        private fun parseSerial(card: ChinaCard): String? =
            ChinaTransitData.getFile(card, 0xa)?.binaryData?.getHexString(0, 5)
    }
}
