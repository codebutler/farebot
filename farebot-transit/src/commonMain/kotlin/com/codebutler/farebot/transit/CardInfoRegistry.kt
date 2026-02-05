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

/**
 * Registry that collects [CardInfo] metadata from all registered [TransitFactory] instances.
 *
 * This provides methods to retrieve the list of all supported cards, either
 * alphabetically sorted or grouped by region.
 */
class CardInfoRegistry(
    private val factories: List<TransitFactory<out Card, out TransitInfo>>
) {
    /**
     * All card info from all registered factories.
     */
    val allCards: List<CardInfo> by lazy {
        factories.flatMap { it.allCards }
    }

    /**
     * All cards sorted alphabetically by name.
     */
    val allCardsAlphabetical: List<CardInfo>
        get() = allCards
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            .distinctBy { it.name }

    /**
     * All cards grouped by region, with regions sorted by priority and then alphabetically.
     *
     * Each pair contains the region and the list of cards in that region (sorted alphabetically).
     */
    val allCardsByRegion: List<Pair<TransitRegion, List<CardInfo>>>
        get() {
            val cards = allCards.distinctBy { it.name }
            val regions = cards.map { it.region }
                .distinct()
                .sortedWith(TransitRegion.RegionComparator)
            return regions.map { region ->
                Pair(
                    region,
                    cards.filter { it.region == region }
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                )
            }
        }

    companion object {
        /**
         * Creates a CardInfoRegistry from a collection of factories.
         *
         * This is a convenience factory method for cases where factories
         * are stored in different data structures.
         */
        fun fromFactories(vararg factories: TransitFactory<out Card, out TransitInfo>): CardInfoRegistry {
            return CardInfoRegistry(factories.toList())
        }
    }
}
