/*
 * SuicaUtilTest.kt
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

package com.codebutler.farebot.transit.suica

import com.codebutler.farebot.base.util.FormattedString
import farebot.transit.suica.generated.resources.Res
import farebot.transit.suica.generated.resources.card_name_hayakaken
import farebot.transit.suica.generated.resources.card_name_icoca
import farebot.transit.suica.generated.resources.card_name_japan_ic
import farebot.transit.suica.generated.resources.card_name_kitaca
import farebot.transit.suica.generated.resources.card_name_manaca
import farebot.transit.suica.generated.resources.card_name_nimoca
import farebot.transit.suica.generated.resources.card_name_pasmo
import farebot.transit.suica.generated.resources.card_name_pitapa
import farebot.transit.suica.generated.resources.card_name_suica
import farebot.transit.suica.generated.resources.card_name_toica
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.StringResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Helper to assert that a FormattedString is a Resource wrapping the expected StringResource.
 */
private fun assertResourceEquals(
    expected: StringResource,
    actual: FormattedString,
    message: String? = null,
) {
    assertTrue(actual is FormattedString.Resource, message ?: "Expected FormattedString.Resource but got $actual")
    assertEquals(expected.key, actual.resource.key, message ?: "Resource key mismatch")
}

class SuicaUtilTest {
    @Test
    fun testSuicaCardDetection() {
        val suicaServices = setOf(0x1808, 0x180a, 0x1848, 0x184b, 0x18c8, 0x18ca)
        assertResourceEquals(Res.string.card_name_suica, SuicaUtil.getCardName(suicaServices))
    }

    @Test
    fun testPASMOCardDetection() {
        val pasmoServices = setOf(0x1848, 0x184b, 0x1cc8, 0x1cca, 0x1d08, 0x1d0a)
        assertResourceEquals(Res.string.card_name_pasmo, SuicaUtil.getCardName(pasmoServices))
    }

    @Test
    fun testICOCACardDetection() {
        val icocaServices = setOf(0x1a48, 0x1a4a, 0x1a88, 0x1a8a, 0x9608, 0x960a)
        assertResourceEquals(Res.string.card_name_icoca, SuicaUtil.getCardName(icocaServices))
    }

    @Test
    fun testTOICACardDetection() {
        val toicaServices = setOf(0x1848, 0x184b, 0x1e08, 0x1e0a, 0x1e48, 0x1e4a)
        assertResourceEquals(Res.string.card_name_toica, SuicaUtil.getCardName(toicaServices))
    }

    @Test
    fun testManacaCardDetection() {
        val manacaServices = setOf(0x9888, 0x988b, 0x98cc, 0x98cf)
        assertResourceEquals(Res.string.card_name_manaca, SuicaUtil.getCardName(manacaServices))
    }

    @Test
    fun testKitacaCardDetection() {
        val kitacaServices = setOf(0x1848, 0x184b, 0x2088, 0x208b, 0x20c8, 0x20cb)
        assertResourceEquals(Res.string.card_name_kitaca, SuicaUtil.getCardName(kitacaServices))
    }

    @Test
    fun testPiTaPaCardDetection() {
        val pitapaServices = setOf(0x1b88, 0x1b8a, 0x9748, 0x974a)
        assertResourceEquals(Res.string.card_name_pitapa, SuicaUtil.getCardName(pitapaServices))
    }

    @Test
    fun testSuicaDetectionFailsWithReadOnlyServicesOnly() {
        val readOnlyServices = setOf(0x090f, 0x108f)
        assertResourceEquals(Res.string.card_name_japan_ic, SuicaUtil.getCardName(readOnlyServices))
    }

    @Test
    fun testUnknownCardReturnsJapanIC() {
        assertResourceEquals(Res.string.card_name_japan_ic, SuicaUtil.getCardName(emptySet()))
        assertResourceEquals(Res.string.card_name_japan_ic, SuicaUtil.getCardName(setOf(0x1234, 0x5678)))
    }

    @Test
    fun testHayakakenCardDetection() {
        val hayakakenServices =
            setOf(
                0x48, 0x4a, 0x88, 0x8b, 0xc8, 0xca, 0xcc, 0xce, 0xd0, 0xd2, 0xd4, 0xd6,
                0x810, 0x812, 0x816, 0x850, 0x852, 0x856, 0x890, 0x892, 0x896,
                0x8c8, 0x8ca, 0x90a, 0x90c, 0x90f,
                0x1008, 0x100a, 0x1048, 0x104a, 0x108c, 0x108f, 0x10c8, 0x10cb,
                0x1108, 0x110a, 0x1148, 0x114a,
                0x1f88, 0x1f8a,
                0x2048, 0x204a, 0x2448, 0x244a, 0x2488, 0x248a, 0x24c8, 0x24ca, 0x2508, 0x250a, 0x2548, 0x254a,
            )
        assertResourceEquals(Res.string.card_name_hayakaken, SuicaUtil.getCardName(hayakakenServices))
    }

    @Test
    fun testNimocaCardDetection() {
        val nimocaServices =
            setOf(
                0x48, 0x4a, 0x88, 0x8b, 0xc8, 0xca, 0xcc, 0xce, 0xd0, 0xd2, 0xd4, 0xd6,
                0x810, 0x812, 0x816, 0x850, 0x852, 0x856, 0x890, 0x892, 0x896,
                0x8c8, 0x8ca, 0x90a, 0x90c, 0x90f,
                0x1008, 0x100a, 0x1048, 0x104a, 0x108c, 0x108f, 0x10c8, 0x10cb,
                0x1108, 0x110a, 0x1148, 0x114a,
                0x1f48, 0x1f4a, 0x1f88, 0x1f8a, 0x1fc8, 0x1fca, 0x2008, 0x200a, 0x2048, 0x204a,
            )
        assertResourceEquals(Res.string.card_name_nimoca, SuicaUtil.getCardName(nimocaServices))
    }

    @Test
    fun testAmbiguousCardReturnsJapanIC() {
        assertResourceEquals(
            Res.string.card_name_japan_ic,
            SuicaUtil.getCardName(setOf(0x8b, 0x90f, 0x108f, 0x10cb)),
        )

        assertResourceEquals(
            Res.string.card_name_japan_ic,
            SuicaUtil.getCardName(
                setOf(0x8b, 0x90f, 0x108f, 0x10cb, 0x184b, 0x194b, 0x234b, 0x238b, 0x23cb),
            ),
        )
    }

    @Test
    fun testExtractDateNullForZero() {
        val data = ByteArray(16)
        val result = SuicaUtil.extractDate(false, data)
        assertEquals(null, result)
    }

    @Test
    fun testExtractDate() {
        val data = ByteArray(16)
        data[4] = 0x28.toByte()
        data[5] = 0xCF.toByte()
        val result = SuicaUtil.extractDate(false, data)
        assertNotNull(result)
        val expected =
            LocalDateTime(2020, 6, 15, 0, 0)
                .toInstant(TimeZone.of("Asia/Tokyo"))
        assertEquals(expected, result)
    }
}
