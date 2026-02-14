/*
 * IosUltralightTagReader.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.ultralight

import com.codebutler.farebot.card.nfc.UltralightTechnology
import com.codebutler.farebot.card.ultralight.raw.RawUltralightCard
import kotlin.time.Clock

/**
 * iOS implementation of the Ultralight tag reader.
 *
 * MIFARE Ultralight cards appear as NFCMiFareTag with mifareFamily == NFCMiFareUltralight.
 * The [UltralightTechnology] wraps the iOS tag's read operations, and the reading
 * logic is the same as Android.
 */
class IosUltralightTagReader(
    private val tagId: ByteArray,
    private val tech: UltralightTechnology,
) {
    fun readTag(): RawUltralightCard {
        tech.connect()
        try {
            val size: Int =
                when (tech.type) {
                    UltralightTechnology.TYPE_ULTRALIGHT -> UltralightCard.ULTRALIGHT_SIZE
                    UltralightTechnology.TYPE_ULTRALIGHT_C -> UltralightCard.ULTRALIGHT_C_SIZE
                    else -> throw IllegalArgumentException("Unknown Ultralight type ${tech.type}")
                }

            var pageNumber = 0
            var pageBuffer = ByteArray(0)
            val pages = mutableListOf<UltralightPage>()
            while (pageNumber <= size) {
                if (pageNumber % 4 == 0) {
                    pageBuffer = tech.readPages(pageNumber)
                }
                pages.add(
                    UltralightPage.create(
                        pageNumber,
                        pageBuffer.copyOfRange(
                            (pageNumber % 4) * UltralightTechnology.PAGE_SIZE,
                            ((pageNumber % 4) + 1) * UltralightTechnology.PAGE_SIZE,
                        ),
                    ),
                )
                pageNumber++
            }

            return RawUltralightCard.create(tagId, Clock.System.now(), pages, tech.type)
        } finally {
            if (tech.isConnected) {
                try {
                    tech.close()
                } catch (_: Exception) {
                }
            }
        }
    }
}
