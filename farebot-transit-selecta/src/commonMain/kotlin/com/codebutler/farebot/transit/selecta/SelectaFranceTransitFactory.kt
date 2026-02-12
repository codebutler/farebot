/*
 * SelectaFranceTransitFactory.kt
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

package com.codebutler.farebot.transit.selecta

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_selecta.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Selecta payment cards (France).
 *
 * Reference: https://dyrk.org/2015/09/03/faille-nfc-distributeur-selecta/
 */
class SelectaFranceTransitFactory : TransitFactory<ClassicCard, SelectaFranceTransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false
        return sector0.getBlock(1).data.byteArrayToInt(2, 2) == 0x0938
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val serial = getSerial(card)
        return TransitIdentity.create(runBlocking { getString(Res.string.selecta_card_name) }, serial.toString())
    }

    override fun parseInfo(card: ClassicCard): SelectaFranceTransitInfo {
        val sector1 = card.getSector(1) as DataClassicSector
        return SelectaFranceTransitInfo(
            serial = getSerial(card),
            balanceValue = sector1.getBlock(2).data.byteArrayToInt(0, 3)
        )
    }

    companion object {
        private val CARD_INFO = CardInfo(
            nameRes = Res.string.selecta_card_name,
            cardType = CardType.MifareClassic,
            region = TransitRegion.FRANCE,
            locationRes = Res.string.selecta_location,
            imageRes = Res.drawable.selecta,
            brandColor = 0xD9423A,
        )
    }

    private fun getSerial(card: ClassicCard): Int {
        return (card.getSector(1) as DataClassicSector).getBlock(0).data.byteArrayToInt(13, 3)
    }
}
