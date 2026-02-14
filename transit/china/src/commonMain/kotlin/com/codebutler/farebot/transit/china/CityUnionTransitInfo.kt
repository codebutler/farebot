/*
 * CityUnionTransitInfo.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/CityUnion.java
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

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.china.ChinaCard
import com.codebutler.farebot.card.china.ChinaCardTransitFactory
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.TransitRegion
import farebot.transit.china.generated.resources.Res
import farebot.transit.china.generated.resources.card_location_china
import farebot.transit.china.generated.resources.card_location_shanghai_china
import farebot.transit.china.generated.resources.card_name_city_union
import farebot.transit.china.generated.resources.card_name_cityunion
import farebot.transit.china.generated.resources.card_name_shanghai
import farebot.transit.china.generated.resources.card_name_shanghai_public_transportation_card
import farebot.transit.china.generated.resources.city_union
import farebot.transit.china.generated.resources.city_union_city
import farebot.transit.china.generated.resources.location_shanghai
import farebot.transit.china.generated.resources.shanghai
import farebot.transit.china.generated.resources.unknown_format
import kotlinx.serialization.Serializable

/**
 * Transit info implementation for China City Union cards.
 *
 * City Union is a smart card platform used in multiple Chinese cities. Each city has its
 * own branding, but they share the same underlying infrastructure. Currently known cities:
 * - Shanghai (上海公共交通卡)
 */
@Serializable
class CityUnionTransitInfo(
    val validityStart: Int?,
    val validityEnd: Int?,
    override val trips: List<ChinaTrip>?,
    val mBalance: Int?,
    private val mSerial: Int?,
    private val mCity: Int?,
) : TransitInfo() {
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

    override val serialNumber: String
        get() = mSerial.toString()

    override val cardName: String
        get() = nameCity(mCity)

    override val info: List<ListItemInterface>?
        get() {
            if (mCity == null) {
                return null
            }
            val cityInfo = cities[mCity]
            return if (cityInfo != null) {
                listOf(
                    ListItem(
                        getStringBlocking(Res.string.city_union_city),
                        getStringBlocking(cityInfo.locationId),
                    ),
                )
            } else {
                listOf(
                    ListItem(
                        getStringBlocking(Res.string.city_union_city),
                        getStringBlocking(Res.string.unknown_format, mCity.toString(16)),
                    ),
                )
            }
        }

    companion object {
        private const val SHANGHAI = 0x2000

        private data class CityInfo(
            val nameId: org.jetbrains.compose.resources.StringResource,
            val locationId: org.jetbrains.compose.resources.StringResource,
        )

        private val cities =
            mapOf(
                SHANGHAI to CityInfo(Res.string.card_name_shanghai, Res.string.location_shanghai),
            )

        private fun parse(card: ChinaCard): CityUnionTransitInfo {
            val file15 = ChinaTransitData.getFile(card, 0x15)?.binaryData
            val (serial, city) = parseSerialAndCity(card)

            return CityUnionTransitInfo(
                mSerial = serial,
                validityStart = file15?.byteArrayToInt(20, 4),
                validityEnd = file15?.byteArrayToInt(24, 4),
                mCity = city,
                mBalance = ChinaTransitData.parseBalance(card),
                trips = ChinaTransitData.parseTrips(card) { ChinaTrip(it) },
            )
        }

        @OptIn(ExperimentalStdlibApi::class)
        val FACTORY: ChinaCardTransitFactory =
            object : ChinaCardTransitFactory {
                override val allCards: List<CardInfo> =
                    listOf(
                        CardInfo(
                            nameRes = Res.string.card_name_city_union,
                            cardType = CardType.ISO7816,
                            region = TransitRegion.CHINA,
                            locationRes = Res.string.card_location_china,
                            imageRes = Res.drawable.city_union,
                            latitude = 39.9042f,
                            longitude = 116.4074f,
                            brandColor = 0x5494B6,
                            credits = listOf("Metrodroid Project", "Vladimir Serbinenko", "Sinpo Lib"),
                        ),
                        CardInfo(
                            nameRes = Res.string.card_name_shanghai_public_transportation_card,
                            cardType = CardType.ISO7816,
                            region = TransitRegion.CHINA,
                            locationRes = Res.string.card_location_shanghai_china,
                            imageRes = Res.drawable.shanghai,
                            latitude = 31.2304f,
                            longitude = 121.4737f,
                            brandColor = 0x1777EA,
                            credits = listOf("Metrodroid Project", "Vladimir Serbinenko", "Sinpo Lib"),
                        ),
                    )

                override val appNames: List<ByteArray>
                    get() = listOf("A00000000386980701".hexToByteArray())

                override fun parseTransitIdentity(card: ChinaCard): TransitIdentity {
                    val (serial, city) = parseSerialAndCity(card)
                    return TransitIdentity(nameCity(city), serial.toString())
                }

                override fun parseTransitData(card: ChinaCard): TransitInfo = parse(card)
            }

        private fun nameCity(city: Int?): String {
            val cityInfo = cities[city]
            return if (cityInfo != null) {
                getStringBlocking(cityInfo.nameId)
            } else {
                getStringBlocking(Res.string.card_name_cityunion)
            }
        }

        private fun parseSerialAndCity(card: ChinaCard): Pair<Int?, Int?> {
            val file15 = ChinaTransitData.getFile(card, 0x15)?.binaryData
            val city = file15?.byteArrayToInt(2, 2)
            return if (city == SHANGHAI) {
                Pair(file15.byteArrayToInt(16, 4), city)
            } else {
                Pair(file15?.byteArrayToIntReversed(16, 4) ?: 0, city)
            }
        }
    }
}
