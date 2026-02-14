/*
 * TransitFactoryRegistry.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.shared.transit

import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo

class TransitFactoryRegistry {

    private val registry = mutableMapOf<CardType, MutableList<TransitFactory<Card, TransitInfo>>>()

    val allCards: List<CardInfo>
        get() = registry.values.flatten().flatMap { it.allCards }

    fun parseTransitIdentity(card: Card): TransitIdentity? = findFactory(card)?.parseIdentity(card)

    fun parseTransitInfo(card: Card): TransitInfo? = findFactory(card)?.parseInfo(card)

    fun findCardInfo(card: Card): CardInfo? =
        findFactory(card)?.allCards?.firstOrNull()

    fun findBrandColor(card: Card): Int? = findCardInfo(card)?.brandColor

    @Suppress("UNCHECKED_CAST")
    fun registerFactory(cardType: CardType, factory: TransitFactory<*, *>) {
        val factories = registry.getOrPut(cardType) { mutableListOf() }
        factories.add(factory as TransitFactory<Card, TransitInfo>)
    }

    private fun findFactory(card: Card): TransitFactory<Card, out TransitInfo>? =
        registry[card.cardType]?.find { it.check(card) }
}
