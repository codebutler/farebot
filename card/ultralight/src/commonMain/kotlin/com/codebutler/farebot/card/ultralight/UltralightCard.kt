/*
 * UltralightCard.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Contains improvements ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.card.ultralight

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import farebot.card.ultralight.generated.resources.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Utility class for reading Mifare Ultralight / Ultralight C
 */
@Serializable
data class UltralightCard(
    @Contextual override val tagId: ByteArray,
    override val scannedAt: Instant,
    val pages: List<UltralightPage>,
    /**
     * Get the type of Ultralight card this is.  This is either MifareUltralight.TYPE_ULTRALIGHT,
     * or MifareUltralight.TYPE_ULTRALIGHT_C.
     *
     * @return Type of Ultralight card this is.
     */
    val ultralightType: Int,
) : Card() {
    override val cardType: CardType = CardType.MifareUltralight

    fun getPage(index: Int): UltralightPage = pages[index]

    /**
     * Read consecutive pages as a single ByteArray.
     */
    fun readPages(
        startPage: Int,
        count: Int,
    ): ByteArray {
        val result = ByteArray(count * 4)
        for (i in 0 until count) {
            pages[startPage + i].data.copyInto(result, i * 4)
        }
        return result
    }

    override suspend fun getAdvancedUi(): FareBotUiTree {
        val builder = FareBotUiTree.builder()
        val pagesBuilder =
            builder
                .item()
                .title(Res.string.ultralight_pages)
        for (page in pages) {
            pagesBuilder
                .item()
                .title(FormattedString(Res.string.ultralight_page_title_format, page.index.toString()))
                .value(page.data)
        }
        return builder.build()
    }

    /**
     * Known Ultralight card type variants, detected via GET_VERSION command.
     */
    enum class UltralightType(
        val pageCount: Int,
    ) {
        UNKNOWN(-1),
        MF0ICU1(16), // MIFARE Ultralight
        MF0ICU2(44), // MIFARE Ultralight C
        EV1_MF0UL11(20), // MIFARE Ultralight EV1 (48 bytes)
        EV1_MF0UL21(41), // MIFARE Ultralight EV1 (128 bytes)
        NTAG213(45),
        NTAG215(135),
        NTAG216(231),
    }

    companion object {
        const val ULTRALIGHT_SIZE = 0x0F
        const val ULTRALIGHT_C_SIZE = 0x2B

        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            pages: List<UltralightPage>,
            type: Int,
        ): UltralightCard = UltralightCard(tagId, scannedAt, pages, type)
    }
}
