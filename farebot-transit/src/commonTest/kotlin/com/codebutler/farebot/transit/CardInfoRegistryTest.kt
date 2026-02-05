/*
 * CardInfoRegistryTest.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2024 Eric Butler <eric@codebutler.com>
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
import com.codebutler.farebot.card.CardType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardInfoRegistryTest {

    @Test
    fun testEmptyRegistry() {
        val registry = CardInfoRegistry(emptyList())
        assertEquals(0, registry.allCards.size)
        assertEquals(0, registry.allCardsAlphabetical.size)
        assertEquals(0, registry.allCardsByRegion.size)
    }

    @Test
    fun testSingleFactory() {
        val factory = TestFactory(
            listOf(
                CardInfo(
                    name = "Test Card",
                    cardType = CardType.MifareDesfire,
                    region = TransitRegion.USA
                )
            )
        )
        val registry = CardInfoRegistry(listOf(factory))
        assertEquals(1, registry.allCards.size)
        assertEquals("Test Card", registry.allCards[0].name)
    }

    @Test
    fun testAlphabeticalSorting() {
        val factory = TestFactory(
            listOf(
                CardInfo(name = "Zeta Card", cardType = CardType.MifareDesfire, region = TransitRegion.USA),
                CardInfo(name = "Alpha Card", cardType = CardType.MifareDesfire, region = TransitRegion.USA),
                CardInfo(name = "Beta Card", cardType = CardType.MifareDesfire, region = TransitRegion.USA)
            )
        )
        val registry = CardInfoRegistry(listOf(factory))
        val cards = registry.allCardsAlphabetical
        assertEquals(3, cards.size)
        assertEquals("Alpha Card", cards[0].name)
        assertEquals("Beta Card", cards[1].name)
        assertEquals("Zeta Card", cards[2].name)
    }

    @Test
    fun testGroupByRegion() {
        val factory = TestFactory(
            listOf(
                CardInfo(name = "Clipper", cardType = CardType.MifareDesfire, region = TransitRegion.USA),
                CardInfo(name = "ORCA", cardType = CardType.MifareDesfire, region = TransitRegion.USA),
                CardInfo(name = "Opal", cardType = CardType.MifareDesfire, region = TransitRegion.AUSTRALIA),
                CardInfo(name = "Suica", cardType = CardType.FeliCa, region = TransitRegion.JAPAN)
            )
        )
        val registry = CardInfoRegistry(listOf(factory))
        val byRegion = registry.allCardsByRegion

        assertEquals(3, byRegion.size)

        // Regions should be sorted alphabetically (Australia, Japan, USA)
        assertEquals("Australia", byRegion[0].first.translatedName)
        assertEquals("Japan", byRegion[1].first.translatedName)
        assertEquals("United States", byRegion[2].first.translatedName)

        // Cards within each region should be alphabetically sorted
        assertEquals(listOf("Opal"), byRegion[0].second.map { it.name })
        assertEquals(listOf("Suica"), byRegion[1].second.map { it.name })
        assertEquals(listOf("Clipper", "ORCA"), byRegion[2].second.map { it.name })
    }

    @Test
    fun testWorldwideRegionFirst() {
        val factory = TestFactory(
            listOf(
                CardInfo(name = "Clipper", cardType = CardType.MifareDesfire, region = TransitRegion.USA),
                CardInfo(name = "EMV", cardType = CardType.ISO7816, region = TransitRegion.WORLDWIDE)
            )
        )
        val registry = CardInfoRegistry(listOf(factory))
        val byRegion = registry.allCardsByRegion

        assertEquals(2, byRegion.size)
        // Worldwide should come before USA (it has a lower section priority)
        assertEquals("Worldwide", byRegion[0].first.translatedName)
        assertEquals("United States", byRegion[1].first.translatedName)
    }

    @Test
    fun testDistinctCardsByName() {
        val factory1 = TestFactory(
            listOf(CardInfo(name = "Clipper", cardType = CardType.MifareDesfire, region = TransitRegion.USA))
        )
        val factory2 = TestFactory(
            listOf(CardInfo(name = "Clipper", cardType = CardType.MifareUltralight, region = TransitRegion.USA))
        )
        val registry = CardInfoRegistry(listOf(factory1, factory2))

        // allCardsAlphabetical should dedupe by name
        assertEquals(1, registry.allCardsAlphabetical.size)
    }

    @Test
    fun testTransitRegionIsoCode() {
        val usa = TransitRegion.Iso("US")
        assertEquals("United States", usa.translatedName)

        val unknown = TransitRegion.Iso("XX")
        assertEquals("XX", unknown.translatedName)
    }

    @Test
    fun testTransitRegionComparator() {
        val regions = listOf(
            TransitRegion.USA,
            TransitRegion.WORLDWIDE,
            TransitRegion.AUSTRALIA,
            TransitRegion.JAPAN
        )
        val sorted = regions.sortedWith(TransitRegion.RegionComparator)

        // WORLDWIDE has section -1 (higher priority), others are section 0
        assertEquals("Worldwide", sorted[0].translatedName)
        // Rest alphabetically: Australia, Japan, United States
        assertEquals("Australia", sorted[1].translatedName)
        assertEquals("Japan", sorted[2].translatedName)
        assertEquals("United States", sorted[3].translatedName)
    }

    private class TestFactory(
        override val allCards: List<CardInfo>
    ) : TransitFactory<Card, TransitInfo> {
        override fun check(card: Card): Boolean = false
        override fun parseIdentity(card: Card): TransitIdentity = TransitIdentity.create("Test", null)
        override fun parseInfo(card: Card): TransitInfo = throw NotImplementedError()
    }
}
