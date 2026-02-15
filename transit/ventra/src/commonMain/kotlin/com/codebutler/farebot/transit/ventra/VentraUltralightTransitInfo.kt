/*
 * VentraUltralightTransitInfo.kt
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

package com.codebutler.farebot.transit.ventra

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.nextfareul.NextfareUltralightTransitData
import com.codebutler.farebot.transit.nextfareul.NextfareUltralightTransitDataCapsule
import farebot.transit.ventra.generated.resources.*
import kotlinx.datetime.TimeZone
import com.codebutler.farebot.base.util.FormattedString

class VentraUltralightTransitInfo(
    override val capsule: NextfareUltralightTransitDataCapsule,
) : NextfareUltralightTransitData() {
    override val cardName: FormattedString
        get() = FormattedString(Res.string.ventra_card_name)

    override val timeZone: TimeZone
        get() = TZ

    override fun makeCurrency(value: Int) = TransitCurrency.USD(value)

    override fun getProductName(productCode: Int): FormattedString? = null

    companion object {
        internal val TZ = TimeZone.of("America/Chicago")

        private val NAME: FormattedString
            get() = FormattedString(Res.string.ventra_card_name)

        val FACTORY: TransitFactory<UltralightCard, VentraUltralightTransitInfo> =
            object : TransitFactory<UltralightCard, VentraUltralightTransitInfo> {
                override val allCards: List<CardInfo> =
                    listOf(
                        CardInfo(
                            nameRes = Res.string.ventra_card_name,
                            cardType = CardType.MifareUltralight,
                            region = TransitRegion.USA,
                            locationRes = Res.string.card_location_chicago_il,
                            extraNoteRes = Res.string.card_note_ventra,
                            imageRes = Res.drawable.ventra,
                            latitude = 41.8781f,
                            longitude = -87.6298f,
                            sampleDumpFile = "Ventra.json",
                            brandColor = 0x0094C8,
                            credits = listOf("Metrodroid Project"),
                        ),
                    )

                override fun check(card: UltralightCard): Boolean {
                    val head = card.getPage(4).data.byteArrayToInt(0, 3)
                    if (head != 0x0a0400 && head != 0x0a0800) {
                        return false
                    }
                    val page1 = card.getPage(5).data
                    if (page1[1].toInt() != 1 || page1[2].toInt() and 0x80 == 0x80 || page1[3].toInt() != 0) {
                        return false
                    }
                    val page2 = card.getPage(6).data
                    return page2.byteArrayToInt(0, 3) == 0
                }

                override fun parseInfo(card: UltralightCard): VentraUltralightTransitInfo =
                    VentraUltralightTransitInfo(
                        parse(card, ::VentraUltralightTransaction),
                    )

                override fun parseIdentity(card: UltralightCard): TransitIdentity =
                    TransitIdentity(NAME, formatSerial(getSerial(card)))
            }
    }
}
