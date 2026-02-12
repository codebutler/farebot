/*
 * BlankClassicTransitFactory.kt
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

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.card.classic.InvalidClassicSector
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_serialonly.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Detects blank MIFARE Classic cards with no meaningful data.
 * This factory should be registered near the END of the Classic factory list,
 * just before UnauthorizedClassicTransitFactory.
 */
class BlankClassicTransitFactory : TransitFactory<ClassicCard, BlankClassicTransitInfo> {

    override val allCards: List<CardInfo> = emptyList()

    /**
     * @param card Card to read.
     * @return true if all sectors on the card are blank.
     */
    override fun check(card: ClassicCard): Boolean {
        val sectors = card.sectors
        var allZero = true
        var allFF = true

        // Check to see if all sectors are blocked
        for ((secIdx, s) in sectors.withIndex()) {
            if (s is UnauthorizedClassicSector || s is InvalidClassicSector) {
                return false
            }

            val dataSector = s as? DataClassicSector ?: continue
            val numBlocks = dataSector.blocks.size

            for ((blockIdx, bl) in dataSector.blocks.withIndex()) {
                // Manufacturer data (sector 0, block 0)
                if (secIdx == 0 && blockIdx == 0) {
                    continue
                }
                // Trailer block (last block in each sector)
                if (blockIdx == numBlocks - 1) {
                    continue
                }

                val data = bl.data
                if (!data.all { it == 0.toByte() }) {
                    allZero = false
                }
                if (!data.all { it == 0xFF.toByte() }) {
                    allFF = false
                }
                if (!allZero && !allFF) {
                    return false
                }
            }
        }
        return true
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val name = runBlocking { getString(Res.string.blank_mfc_card) }
        return TransitIdentity.create(name, null)
    }

    override fun parseInfo(card: ClassicCard): BlankClassicTransitInfo {
        return BlankClassicTransitInfo()
    }
}

class BlankClassicTransitInfo : TransitInfo() {
    override val cardName: String = runBlocking { getString(Res.string.blank_mfc_card) }

    override val serialNumber: String? = null

    override val info: List<ListItemInterface>
        get() = listOf(
            HeaderListItem(Res.string.fully_blank_title),
            ListItem(Res.string.fully_blank_desc)
        )
}
