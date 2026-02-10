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

@file:OptIn(InternalResourceApi::class)

package com.codebutler.farebot.transit

import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.StringResource
import kotlin.test.Test
import kotlin.test.assertEquals

class CardInfoRegistryTest {

    private fun testStringRes(name: String) = StringResource("string:$name", name, emptySet())

    @Test
    fun testEmptyRegistry() {
        val registry = CardInfoRegistry(emptyList())
        assertEquals(0, registry.allCards.size)
        assertEquals(0, registry.allCardsByRegion.size)
    }

    @Test
    fun testSingleFactory() {
        val factory = TestFactory(
            listOf(
                CardInfo(
                    nameRes = testStringRes("test_card"),
                    cardType = CardType.MifareDesfire,
                    region = TransitRegion.USA,
                    locationRes = testStringRes("location_test"),
                )
            )
        )
        val registry = CardInfoRegistry(listOf(factory))
        assertEquals(1, registry.allCards.size)
    }

    @Test
    fun testGroupByRegion() {
        val factory = TestFactory(
            listOf(
                CardInfo(nameRes = testStringRes("clipper"), cardType = CardType.MifareDesfire, region = TransitRegion.USA, locationRes = testStringRes("sf")),
                CardInfo(nameRes = testStringRes("orca"), cardType = CardType.MifareDesfire, region = TransitRegion.USA, locationRes = testStringRes("seattle")),
                CardInfo(nameRes = testStringRes("opal"), cardType = CardType.MifareDesfire, region = TransitRegion.AUSTRALIA, locationRes = testStringRes("sydney")),
                CardInfo(nameRes = testStringRes("suica"), cardType = CardType.FeliCa, region = TransitRegion.JAPAN, locationRes = testStringRes("tokyo")),
            )
        )
        val registry = CardInfoRegistry(listOf(factory))
        val byRegion = registry.allCardsByRegion

        assertEquals(3, byRegion.size)

        // Regions should be sorted alphabetically (Australia, Japan, USA)
        assertEquals("Australia", byRegion[0].first.translatedName)
        assertEquals("Japan", byRegion[1].first.translatedName)
        assertEquals("United States", byRegion[2].first.translatedName)

        assertEquals(1, byRegion[0].second.size)
        assertEquals(1, byRegion[1].second.size)
        assertEquals(2, byRegion[2].second.size)
    }

    @Test
    fun testWorldwideRegionFirst() {
        val factory = TestFactory(
            listOf(
                CardInfo(nameRes = testStringRes("clipper"), cardType = CardType.MifareDesfire, region = TransitRegion.USA, locationRes = testStringRes("sf")),
                CardInfo(nameRes = testStringRes("emv"), cardType = CardType.ISO7816, region = TransitRegion.WORLDWIDE, locationRes = testStringRes("various")),
            )
        )
        val registry = CardInfoRegistry(listOf(factory))
        val byRegion = registry.allCardsByRegion

        assertEquals(2, byRegion.size)
        assertEquals("Worldwide", byRegion[0].first.translatedName)
        assertEquals("United States", byRegion[1].first.translatedName)
    }

    @Test
    fun testDistinctCardsByNameRes() {
        val sharedRes = testStringRes("clipper")
        val factory1 = TestFactory(
            listOf(CardInfo(nameRes = sharedRes, cardType = CardType.MifareDesfire, region = TransitRegion.USA, locationRes = testStringRes("sf")))
        )
        val factory2 = TestFactory(
            listOf(CardInfo(nameRes = sharedRes, cardType = CardType.MifareUltralight, region = TransitRegion.USA, locationRes = testStringRes("sf")))
        )
        val registry = CardInfoRegistry(listOf(factory1, factory2))
        val byRegion = registry.allCardsByRegion
        assertEquals(1, byRegion.flatMap { it.second }.size)
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

        assertEquals("Worldwide", sorted[0].translatedName)
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
