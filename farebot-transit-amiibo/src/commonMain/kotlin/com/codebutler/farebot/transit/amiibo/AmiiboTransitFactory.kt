/*
 * AmiiboTransitFactory.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.amiibo

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_amiibo.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Nintendo Amiibo NFC tag reader (NTAG215).
 * Ported from Metrodroid.
 *
 * https://3dbrew.org/wiki/Amiibo#Data_structures
 */
class AmiiboTransitFactory : TransitFactory<UltralightCard, AmiiboTransitInfo> {

    override fun check(card: UltralightCard): Boolean {
        // Amiibo uses NTAG215 (135 pages). Check the lock/CC bytes
        // and that pages 0x15-0x16 contain valid amiibo data.
        if (card.pages.size < 0x17) return false
        val page3 = card.getPage(3).data
        // CC header: E1 10 3E 00 (NTAG215 NDEF capability container)
        return page3[0] == 0xE1.toByte() &&
            page3[1] == 0x10.toByte() &&
            page3[2] == 0x3E.toByte() &&
            hasAmiiboData(card)
    }

    private fun hasAmiiboData(card: UltralightCard): Boolean {
        if (card.pages.size <= 0x16) return false
        val page21 = card.getPage(0x15).data
        val page22 = card.getPage(0x16).data
        return !(page21.all { it == 0.toByte() } && page22.all { it == 0.toByte() })
    }

    override fun parseIdentity(card: UltralightCard): TransitIdentity {
        return TransitIdentity.create(runBlocking { getString(Res.string.amiibo_card_name) }, null)
    }

    override fun parseInfo(card: UltralightCard): AmiiboTransitInfo {
        return AmiiboTransitInfo(
            character = card.getPage(0x15).data.byteArrayToInt(0, 2),
            characterVariant = card.getPage(0x15).data.byteArrayToInt(2, 1),
            figureType = card.getPage(0x15).data.byteArrayToInt(3, 1),
            modelNumber = card.getPage(0x16).data.byteArrayToInt(0, 2),
            series = card.getPage(0x16).data.getBitsFromBuffer(16, 8)
        )
    }
}

class AmiiboTransitInfo internal constructor(
    private val character: Int,
    private val characterVariant: Int,
    private val figureType: Int,
    private val modelNumber: Int,
    private val series: Int
) : TransitInfo() {
    override val cardName: String = runBlocking { getString(Res.string.amiibo_card_name) }
    override val serialNumber: String? = null

    private fun figureTypeName(): String = runBlocking {
        when (figureType) {
            0 -> getString(Res.string.amiibo_figure_type_figure)
            1 -> getString(Res.string.amiibo_figure_type_card)
            2 -> getString(Res.string.amiibo_figure_type_yarn)
            else -> getString(Res.string.amiibo_unknown_type, figureType)
        }
    }

    override val info: List<ListItemInterface>
        get() = listOf(
            ListItem(Res.string.amiibo_type, figureTypeName()),
            ListItem(Res.string.amiibo_character, "0x${character.toString(16).padStart(4, '0')}"),
            ListItem(Res.string.amiibo_character_variant, characterVariant.toString()),
            ListItem(Res.string.amiibo_model_number, modelNumber.toString()),
            ListItem(Res.string.amiibo_series, series.toString())
        )
}
