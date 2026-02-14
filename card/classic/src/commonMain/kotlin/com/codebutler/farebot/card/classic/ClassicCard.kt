/*
 * ClassicCard.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import farebot.card.classic.generated.resources.*
import kotlin.time.Instant

class ClassicCard(
    override val tagId: ByteArray,
    override val scannedAt: Instant,
    val sectors: List<ClassicSector>,
    val isPartialRead: Boolean = false,
) : Card() {
    override val cardType: CardType = CardType.MifareClassic

    fun getSector(index: Int): ClassicSector = sectors[index]

    /**
     * Manufacturing information extracted from block 0 of sector 0.
     * Returns null if sector 0 is unauthorized or invalid.
     */
    val manufacturingInfo: ClassicManufacturingInfo?
        get() {
            val sector0 = sectors.firstOrNull() ?: return null
            if (sector0 !is DataClassicSector) return null
            val block0 = sector0.blocks.firstOrNull() ?: return null
            if (block0.data.size < 16) return null
            return ClassicManufacturingInfo.parse(block0.data, tagId)
        }

    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree {
        val cardUiBuilder = FareBotUiTree.builder(stringResource)
        for (sector in sectors) {
            val sectorIndexString = sector.index.toString(16)
            val sectorUiBuilder = cardUiBuilder.item()
            when (sector) {
                is UnauthorizedClassicSector -> {
                    sectorUiBuilder.title(
                        stringResource.getString(
                            Res.string.classic_unauthorized_sector_title_format,
                            sectorIndexString,
                        ),
                    )
                }
                is InvalidClassicSector -> {
                    sectorUiBuilder.title(
                        stringResource.getString(
                            Res.string.classic_invalid_sector_title_format,
                            sectorIndexString,
                            sector.error,
                        ),
                    )
                }
                else -> {
                    val dataClassicSector = sector as DataClassicSector
                    sectorUiBuilder.title(
                        stringResource.getString(Res.string.classic_sector_title_format, sectorIndexString),
                    )
                    for (block in dataClassicSector.blocks) {
                        sectorUiBuilder
                            .item()
                            .title(
                                stringResource.getString(
                                    Res.string.classic_block_title_format,
                                    block.index.toString(),
                                ),
                            ).value(block.data)
                    }
                }
            }
        }
        return cardUiBuilder.build()
    }

    companion object {
        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            sectors: List<ClassicSector>,
            isPartialRead: Boolean = false,
        ): ClassicCard = ClassicCard(tagId, scannedAt, sectors, isPartialRead)
    }
}
