/*
 * TroikaTransitFactory.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.troika

import com.codebutler.farebot.base.util.HashUtils
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import farebot.transit.troika.generated.resources.*
import com.codebutler.farebot.base.util.FormattedString

/**
 * Troika, Moscow, Russia.
 * Multi-layout Classic card with TroikaBlock parsing across sectors 8, 7, 4, 1.
 *
 * Detection uses key-hash matching on sector 1 keys, with fallback to
 * checking the header magic bytes on sector 8 and sector 4.
 */
class TroikaTransitFactory : TransitFactory<ClassicCard, TroikaTransitInfo> {
    // TroikaHybridTransitFactory is the registered factory; this is an internal helper.
    override val allCards: List<CardInfo>
        get() = emptyList()

    override fun check(card: ClassicCard): Boolean {
        // First try key-hash based early detection on sector 1
        if (card.sectors.size >= 2) {
            val sector1 = card.getSector(1)
            if (sector1 is DataClassicSector) {
                val keyMatch =
                    HashUtils.checkKeyHash(
                        sector1.keyA,
                        sector1.keyB,
                        TROIKA_KEY_SALT,
                        TROIKA_KEY_HASH,
                    )
                if (keyMatch >= 0) return true
            }
        }

        // Fallback: check header magic on main data sectors
        return MAIN_BLOCKS.any { idx ->
            if (idx >= card.sectors.size) return@any false
            val sector = card.getSector(idx) as? DataClassicSector ?: return@any false
            try {
                TroikaBlock.check(sector.getBlock(0).data)
            } catch (_: Exception) {
                false
            }
        }
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val block =
            MAIN_BLOCKS.firstNotNullOfOrNull { idx ->
                if (idx >= card.sectors.size) return@firstNotNullOfOrNull null
                val sector = card.getSector(idx) as? DataClassicSector ?: return@firstNotNullOfOrNull null
                try {
                    val data = sector.readBlocks(0, 3)
                    if (TroikaBlock.check(data)) data else null
                } catch (_: Exception) {
                    null
                }
            } ?: throw RuntimeException("No valid Troika sector found")

        return TransitIdentity.create(
            FormattedString(Res.string.card_name_troika),
            TroikaBlock.formatSerial(TroikaBlock.getSerial(block)),
        )
    }

    override fun parseInfo(card: ClassicCard): TroikaTransitInfo {
        val blocks =
            SECTOR_ORDER.mapNotNull { idx ->
                decodeSector(card, idx)?.let { idx to it }
            }
        return TroikaTransitInfo(blocks)
    }

    companion object {
        internal val CARD_NAME: FormattedString
            get() = FormattedString(Res.string.card_name_troika)

        /**
         * Main sectors to check for Troika header magic.
         * Sector 8 is the primary data sector, sector 4 is the secondary.
         */
        private val MAIN_BLOCKS = listOf(8, 4)

        /**
         * Order in which sectors are decoded. The first valid sector
         * determines the serial number and primary balance.
         */
        private val SECTOR_ORDER = listOf(8, 7, 4, 1)

        /**
         * Key hash salt and expected hash for early sector-1 key detection.
         * From Metrodroid: HashUtils.checkKeyHash(sectors[1], "troika", "0045ccfe4749673d77273162e8d53015")
         */
        private const val TROIKA_KEY_SALT = "troika"
        private const val TROIKA_KEY_HASH = "0045ccfe4749673d77273162e8d53015"

        private fun decodeSector(
            card: ClassicCard,
            idx: Int,
        ): TroikaBlock? {
            return try {
                if (idx >= card.sectors.size) return null
                val sector = card.getSector(idx) as? DataClassicSector ?: return null
                val data = sector.readBlocks(0, 3)
                if (!TroikaBlock.check(data)) null else TroikaBlock.parseBlock(data)
            } catch (_: Exception) {
                null
            }
        }
    }
}
