/*
 * ISO7816DispatcherTest.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2026 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.test

import com.codebutler.farebot.card.ksx6924.KSX6924Application
import com.codebutler.farebot.shared.nfc.ISO7816Dispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ISO7816DispatcherTest {
    @Test
    fun testKSX6924FileSelectorsIncludesDf00() {
        val configs = ISO7816Dispatcher.buildAppConfigs()
        val ksx6924Config = configs.find { it.type == KSX6924Application.TYPE }
        assertNotNull(ksx6924Config, "KSX6924 config should be present")

        val fileSelectors = ksx6924Config.fileSelectors
        val fileIds = fileSelectors.map { it.fileId }

        // Files 1-5 should be present
        for (fileId in 1..5) {
            assertTrue(
                fileId in fileIds,
                "File selector for file $fileId should be present",
            )
        }

        // File 0xdf00 should be present (T-Money cards)
        assertTrue(
            0xdf00 in fileIds,
            "File selector for file 0xdf00 should be present",
        )

        // Total should be 6 selectors
        assertEquals(6, fileSelectors.size, "Should have exactly 6 file selectors")

        // All KSX6924 file selectors should have null parentDf
        for (selector in fileSelectors) {
            assertNull(selector.parentDf, "KSX6924 file selectors should have null parentDf")
        }
    }
}
