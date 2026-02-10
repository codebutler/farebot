/*
 * CardInfoRegistry.kt
 *
 * Copyright 2019 Google
 * Copyright 2024 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit

import com.codebutler.farebot.card.Card

class CardInfoRegistry(
    private val factories: List<TransitFactory<out Card, out TransitInfo>>
) {
    val allCards: List<CardInfo> by lazy {
        factories.flatMap { it.allCards }
    }

    val allCardsByRegion: List<Pair<TransitRegion, List<CardInfo>>>
        get() {
            val cards = allCards.distinctBy { it.nameRes }
            val regions = cards.map { it.region }
                .distinct()
                .sortedWith(TransitRegion.RegionComparator)
            return regions.map { region ->
                Pair(
                    region,
                    cards.filter { it.region == region }
                )
            }
        }

    companion object {
        fun fromFactories(vararg factories: TransitFactory<out Card, out TransitInfo>): CardInfoRegistry {
            return CardInfoRegistry(factories.toList())
        }
    }
}
