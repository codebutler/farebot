/*
 * NextfareUnknownUltralightTransitInfo.kt
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

package com.codebutler.farebot.transit.nextfareul

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import farebot.farebot_transit_nextfareul.generated.resources.Res
import farebot.farebot_transit_nextfareul.generated.resources.nextfareul_card_name
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.getString

class NextfareUnknownUltralightTransitInfo(
    override val capsule: NextfareUltralightTransitDataCapsule
) : NextfareUltralightTransitData() {

    override val timeZone: TimeZone
        get() = TZ

    override val cardName: String
        get() = runBlocking { getString(Res.string.nextfareul_card_name) }

    override fun makeCurrency(value: Int) = TransitCurrency.XXX(value)

    override fun getProductName(productCode: Int): String? = null

    companion object {
        internal val TZ = TimeZone.UTC

        val FACTORY: TransitFactory<UltralightCard, NextfareUnknownUltralightTransitInfo> =
            object : TransitFactory<UltralightCard, NextfareUnknownUltralightTransitInfo> {

                override fun check(card: UltralightCard): Boolean {
                    val head = card.getPage(4).data.byteArrayToInt(0, 3)
                    return head == 0x0a0400 || head == 0x0a0800
                }

                override fun parseInfo(card: UltralightCard): NextfareUnknownUltralightTransitInfo {
                    return NextfareUnknownUltralightTransitInfo(
                        NextfareUltralightTransitData.parse(card, ::NextfareUnknownUltralightTransaction)
                    )
                }

                override fun parseIdentity(card: UltralightCard): TransitIdentity {
                    return TransitIdentity(
                        runBlocking { getString(Res.string.nextfareul_card_name) },
                        NextfareUltralightTransitData.formatSerial(
                            NextfareUltralightTransitData.getSerial(card)
                        )
                    )
                }
            }
    }
}
