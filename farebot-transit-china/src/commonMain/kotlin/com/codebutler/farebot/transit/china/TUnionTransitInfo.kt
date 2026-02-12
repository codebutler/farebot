/*
 * TUnionTransitInfo.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/TUnion.java
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
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getHexString
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
import farebot.farebot_transit_china.generated.resources.card_location_china
import farebot.farebot_transit_china.generated.resources.card_name_t_union
import farebot.farebot_transit_china.generated.resources.card_name_tunion
import farebot.farebot_transit_china.generated.resources.tunion
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString

/**
 * Transit info implementation for T-Union (交通联合) cards.
 *
 * T-Union is a nationwide interoperable transit card system in China.
 * Cards from participating cities can be used on transit systems in other
 * participating cities across China.
 *
 * The card has a special handling for balance: it stores both positive and
 * negative balance values, with the effective balance being the difference
 * when the primary balance is non-positive.
 */
@Serializable
class TUnionTransitInfo(
    override val serialNumber: String?,
    private val mNegativeBalance: Int,
    private val mBalance: Int,
    override val trips: List<ChinaTrip>?,
    val validityStart: Int?,
    val validityEnd: Int?
) : TransitInfo() {

    override val cardName: String
        get() = runBlocking { getString(Res.string.card_name_tunion) }

    override val balance: TransitBalance
        get() = TransitBalance(
            balance = TransitCurrency.CNY(
                if (mBalance > 0) mBalance else mBalance - mNegativeBalance
            ),
            validFrom = ChinaTransitData.parseHexDate(validityStart),
            validTo = ChinaTransitData.parseHexDate(validityEnd)
        )

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        private fun parse(card: ChinaCard): TUnionTransitInfo? {
            val file15 = ChinaTransitData.getFile(card, 0x15)?.binaryData ?: return null
            return TUnionTransitInfo(
                serialNumber = parseSerial(card),
                validityStart = file15.byteArrayToInt(20, 4),
                validityEnd = file15.byteArrayToInt(24, 4),
                trips = ChinaTransitData.parseTrips(card) { ChinaTrip(it) },
                mBalance = card.getBalance(0)?.getBitsFromBuffer(1, 31) ?: 0,
                mNegativeBalance = card.getBalance(1)?.getBitsFromBuffer(1, 31) ?: 0
            )
        }

        @OptIn(ExperimentalStdlibApi::class)
        val FACTORY: ChinaCardTransitFactory = object : ChinaCardTransitFactory {
            override val allCards: List<CardInfo> = listOf(
                CardInfo(nameRes = Res.string.card_name_t_union, cardType = CardType.ISO7816, region = TransitRegion.CHINA, locationRes = Res.string.card_location_china, imageRes = Res.drawable.tunion, latitude = 39.9042f, longitude = 116.4074f, brandColor = 0xFD0026)
            )

            override val appNames: List<ByteArray>
                get() = listOf("A000000632010105".hexToByteArray())

            override fun parseTransitIdentity(card: ChinaCard): TransitIdentity =
                TransitIdentity(
                    runBlocking { getString(Res.string.card_name_tunion) },
                    parseSerial(card)
                )

            override fun parseTransitData(card: ChinaCard): TransitInfo =
                parse(card) ?: throw IllegalStateException("Failed to parse T-Union card")
        }

        private fun parseSerial(card: ChinaCard): String? =
            ChinaTransitData.getFile(card, 0x15)?.binaryData?.getHexString(10, 10)?.substring(1)
    }
}
