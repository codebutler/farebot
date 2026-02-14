/*
 * NewShenzhenTransitInfo.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/ShenzhenTong.java
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

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.china.ChinaCard
import com.codebutler.farebot.card.china.ChinaCardTransitFactory
import com.codebutler.farebot.card.iso7816.ISO7816TLV
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_china.generated.resources.Res
import farebot.farebot_transit_china.generated.resources.card_location_shenzhen_china
import farebot.farebot_transit_china.generated.resources.card_name_shenzhen_tong
import farebot.farebot_transit_china.generated.resources.card_name_szt
import farebot.farebot_transit_china.generated.resources.szt_card
import kotlinx.serialization.Serializable

/**
 * Transit info implementation for Shenzhen Tong (深圳通) cards.
 *
 * Shenzhen Tong is the primary transit card used in Shenzhen, China. It can be used on:
 * - Shenzhen Metro
 * - Shenzhen buses
 * - Taxis (some)
 * - Retail stores (some)
 */
@Serializable
class NewShenzhenTransitInfo(
    val validityStart: Int?,
    val validityEnd: Int?,
    private val mSerial: Int,
    override val trips: List<NewShenzhenTrip>?,
    val mBalance: Int?,
) : TransitInfo() {
    override val serialNumber: String?
        get() = formatSerial(mSerial)

    override val cardName: String
        get() = getStringBlocking(Res.string.card_name_szt)

    override val balance: TransitBalance?
        get() =
            if (mBalance != null) {
                TransitBalance(
                    balance = TransitCurrency.CNY(mBalance),
                    validFrom = ChinaTransitData.parseHexDate(validityStart),
                    validTo = ChinaTransitData.parseHexDate(validityEnd),
                )
            } else {
                null
            }

    companion object {
        private fun parse(card: ChinaCard): NewShenzhenTransitInfo {
            val szttag = getTagInfo(card)

            return NewShenzhenTransitInfo(
                validityStart = szttag?.byteArrayToInt(20, 4),
                validityEnd = szttag?.byteArrayToInt(24, 4),
                trips = ChinaTransitData.parseTrips(card) { NewShenzhenTrip(it) },
                mSerial = parseSerial(card),
                mBalance = ChinaTransitData.parseBalance(card),
            )
        }

        val FACTORY: ChinaCardTransitFactory =
            object : ChinaCardTransitFactory {
                override val allCards: List<CardInfo> =
                    listOf(
                        CardInfo(
                            nameRes = Res.string.card_name_shenzhen_tong,
                            cardType = CardType.ISO7816,
                            region = TransitRegion.CHINA,
                            locationRes = Res.string.card_location_shenzhen_china,
                            imageRes = Res.drawable.szt_card,
                            latitude = 22.5431f,
                            longitude = 114.0579f,
                            brandColor = 0xA8CC01,
                            credits = listOf("Metrodroid Project", "Vladimir Serbinenko", "Sinpo Lib"),
                        ),
                    )

                override val appNames: List<ByteArray>
                    get() = listOf("PAY.SZT".encodeToByteArray())

                override fun parseTransitIdentity(card: ChinaCard): TransitIdentity =
                    TransitIdentity(
                        getStringBlocking(Res.string.card_name_szt),
                        formatSerial(parseSerial(card)),
                    )

                override fun parseTransitData(card: ChinaCard): TransitInfo = parse(card)
            }

        private fun formatSerial(sn: Int): String {
            val digsum = NumberUtils.getDigitSum(sn.toLong())
            // Sum of digits must be divisible by 10
            val lastDigit = (10 - digsum % 10) % 10
            return "$sn($lastDigit)"
        }

        private fun getTagInfo(card: ChinaCard): ByteArray? {
            val file15 = ChinaTransitData.getFile(card, 0x15)
            if (file15 != null) {
                return file15.binaryData
            }
            val szttag = card.appProprietaryBerTlv ?: return null
            return ISO7816TLV.findBERTLV(szttag, "8c", false)
        }

        private fun parseSerial(card: ChinaCard): Int = getTagInfo(card)?.byteArrayToIntReversed(16, 4) ?: 0
    }
}
