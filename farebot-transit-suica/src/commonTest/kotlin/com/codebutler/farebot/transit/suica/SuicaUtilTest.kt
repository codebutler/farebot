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

import com.codebutler.farebot.base.util.StringResource
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.StringResource as ComposeStringResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test implementation of StringResource that returns the resource key name.
 */
private class TestStringResource : StringResource {
    override fun getString(resource: ComposeStringResource): String {
        return resource.key
    }

    override fun getString(resource: ComposeStringResource, vararg formatArgs: Any): String {
        return "${resource.key}: ${formatArgs.joinToString(", ")}"
    }
}

class SuicaUtilTest {

    private val stringResource = TestStringResource()

    @Test
    fun testSuicaCardDetection() {
        // Suica has unique services: 0x1808, 0x180a, 0x18c8, 0x18ca
        val suicaServices = setOf(0x1808, 0x180a, 0x1848, 0x184b, 0x18c8, 0x18ca)
        assertEquals("card_name_suica", SuicaUtil.getCardName(stringResource, suicaServices))
    }

    @Test
    fun testPASMOCardDetection() {
        // PASMO has unique services: 0x1cc8, 0x1cca, 0x1d08, 0x1d0a
        val pasmoServices = setOf(0x1848, 0x184b, 0x1cc8, 0x1cca, 0x1d08, 0x1d0a)
        assertEquals("card_name_pasmo", SuicaUtil.getCardName(stringResource, pasmoServices))
    }

    @Test
    fun testICOCACardDetection() {
        // ICOCA has unique services: 0x1a48, 0x1a4a, 0x1a88, 0x1a8a, 0x9608, 0x960a
        val icocaServices = setOf(0x1a48, 0x1a4a, 0x1a88, 0x1a8a, 0x9608, 0x960a)
        assertEquals("card_name_icoca", SuicaUtil.getCardName(stringResource, icocaServices))
    }

    @Test
    fun testTOICACardDetection() {
        // TOICA has unique services: 0x1e08, 0x1e0a, 0x1e48, etc.
        val toicaServices = setOf(0x1848, 0x184b, 0x1e08, 0x1e0a, 0x1e48, 0x1e4a)
        assertEquals("card_name_toica", SuicaUtil.getCardName(stringResource, toicaServices))
    }

    @Test
    fun testManacaCardDetection() {
        // manaca has unique services: 0x9888, 0x988b, etc.
        val manacaServices = setOf(0x9888, 0x988b, 0x98cc, 0x98cf)
        assertEquals("card_name_manaca", SuicaUtil.getCardName(stringResource, manacaServices))
    }

    @Test
    fun testKitacaCardDetection() {
        // Kitaca has unique services: 0x2088, 0x208b, 0x20c8, etc.
        val kitacaServices = setOf(0x1848, 0x184b, 0x2088, 0x208b, 0x20c8, 0x20cb)
        assertEquals("card_name_kitaca", SuicaUtil.getCardName(stringResource, kitacaServices))
    }

    @Test
    fun testPiTaPaCardDetection() {
        // PiTaPa has unique services: 0x1b88, 0x1b8a, 0x9748, 0x974a
        val pitapaServices = setOf(0x1b88, 0x1b8a, 0x9748, 0x974a)
        assertEquals("card_name_pitapa", SuicaUtil.getCardName(stringResource, pitapaServices))
    }

    @Test
    fun testSuicaDetectionFailsWithReadOnlyServicesOnly() {
        // When only read-only service codes (attrs 0x09, 0x0B, 0x0F, 0x17) are available,
        // Suica's unique codes (attrs 0x08, 0x0A) are missing and detection falls back to "Japan IC".
        // This was the bug on iOS before expanding the probe attributes.
        val readOnlyServices = setOf(0x090f, 0x108f) // SERVICE_SUICA_HISTORY, SERVICE_SUICA_INOUT
        assertEquals("card_name_japan_ic", SuicaUtil.getCardName(stringResource, readOnlyServices))
    }

    @Test
    fun testUnknownCardReturnsJapanIC() {
        // Empty or unrecognized service codes should return "Japan IC"
        assertEquals("card_name_japan_ic", SuicaUtil.getCardName(stringResource, emptySet()))
        assertEquals("card_name_japan_ic", SuicaUtil.getCardName(stringResource, setOf(0x1234, 0x5678)))
    }

    @Test
    fun testExtractDateNullForZero() {
        // When date bytes are both zero, extractDate should return null
        val data = ByteArray(16)
        val result = SuicaUtil.extractDate(false, data)
        assertEquals(null, result)
    }

    @Test
    fun testExtractDate() {
        // Encode date: year=20 (2020), month=6, day=15
        // date = (20 << 9) | (6 << 5) | 15 = 10240 + 192 + 15 = 10447 = 0x28CF
        val data = ByteArray(16)
        data[4] = 0x28.toByte()
        data[5] = 0xCF.toByte()
        val result = SuicaUtil.extractDate(false, data)
        assertNotNull(result)
        // 2020-06-15T00:00 Asia/Tokyo
        val expected = LocalDateTime(2020, 6, 15, 0, 0)
            .toInstant(TimeZone.of("Asia/Tokyo"))
        assertEquals(expected, result)
    }
}
