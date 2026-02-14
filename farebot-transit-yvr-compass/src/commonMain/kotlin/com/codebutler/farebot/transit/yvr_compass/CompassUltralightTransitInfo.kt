/*
 * CompassUltralightTransitInfo.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.yvr_compass

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.nextfareul.NextfareUltralightTransitData
import com.codebutler.farebot.transit.nextfareul.NextfareUltralightTransitDataCapsule
import farebot.farebot_transit_yvr_compass.generated.resources.*
import kotlinx.datetime.TimeZone

/* Based on reference at http://www.lenrek.net/experiments/compass-tickets/. */
class CompassUltralightTransitInfo(
    override val capsule: NextfareUltralightTransitDataCapsule
) : NextfareUltralightTransitData() {

    override val timeZone: TimeZone
        get() = TZ

    override val cardName: String
        get() = getStringBlocking(Res.string.compass_card_name)

    override fun makeCurrency(value: Int) = TransitCurrency.CAD(value)

    override fun getProductName(productCode: Int): String? = PRODUCT_CODES[productCode]?.let {
        getStringBlocking(it)
    }

    companion object {
        internal val TZ = TimeZone.of("America/Vancouver")

        val FACTORY: TransitFactory<UltralightCard, CompassUltralightTransitInfo> =
            object : TransitFactory<UltralightCard, CompassUltralightTransitInfo> {

                override val allCards: List<CardInfo> = listOf(
                    CardInfo(
                        nameRes = Res.string.compass_card_name,
                        cardType = CardType.MifareUltralight,
                        region = TransitRegion.CANADA,
                        locationRes = Res.string.card_location_vancouver_canada,
                        extraNoteRes = Res.string.card_note_compass,
                        imageRes = Res.drawable.yvr_compass_card,
                        latitude = 49.2827f,
                        longitude = -123.1207f,
                        sampleDumpFile = "Compass.json",
                        brandColor = 0x66B9E1,
                        credits = listOf("Metrodroid Project", "Vladimir Serbinenko", "Toomas Losin"),
                    )
                )

                override fun check(card: UltralightCard): Boolean {
                    val head = card.getPage(4).data.byteArrayToInt(0, 3)
                    if (head != 0x0a0400 && head != 0x0a0800)
                        return false
                    val page1 = card.getPage(5).data
                    if (page1[1].toInt() != 1 || page1[2].toInt() and 0x80 != 0x80 || page1[3].toInt() != 0)
                        return false
                    val page2 = card.getPage(6).data
                    return page2.byteArrayToInt(0, 3) == 0
                }

                override fun parseInfo(card: UltralightCard): CompassUltralightTransitInfo =
                    CompassUltralightTransitInfo(
                        parse(card) { raw, baseDate -> CompassUltralightTransaction(raw, baseDate) }
                    )

                override fun parseIdentity(card: UltralightCard): TransitIdentity =
                    TransitIdentity(
                        getStringBlocking(Res.string.compass_card_name),
                        formatSerial(getSerial(card))
                    )
            }

        private val PRODUCT_CODES = mapOf(
            0x01 to Res.string.compass_product_daypass,
            0x02 to Res.string.compass_product_one_zone,
            0x03 to Res.string.compass_product_two_zone,
            0x04 to Res.string.compass_product_three_zone,
            0x0f to Res.string.compass_product_four_zone_wce,
            0x11 to Res.string.compass_product_sea_island_free,
            0x16 to Res.string.compass_product_exit,
            0x1e to Res.string.compass_product_one_zone_yvr,
            0x1f to Res.string.compass_product_two_zone_yvr,
            0x20 to Res.string.compass_product_three_zone_yvr,
            0x21 to Res.string.compass_product_daypass_yvr,
            0x22 to Res.string.compass_product_bulk_daypass,
            0x23 to Res.string.compass_product_bulk_one_zone,
            0x24 to Res.string.compass_product_bulk_two_zone,
            0x25 to Res.string.compass_product_bulk_three_zone,
            0x26 to Res.string.compass_product_bulk_one_zone,
            0x27 to Res.string.compass_product_bulk_two_zone,
            0x28 to Res.string.compass_product_bulk_three_zone,
            0x29 to Res.string.compass_product_gradpass
        )
    }
}
