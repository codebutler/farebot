/*
 * UltralightCardReader.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

object UltralightCardReader {
    @Throws(Exception::class)
    fun readCard(
        tagId: ByteArray,
        tech: UltralightTechnology,
    ): RawUltralightCard {
        // Detect card type using protocol commands (GET_VERSION, AUTH_1)
        val detectedType = detectCardType(tech)
        println("UltralightCardReader: Detected card type: $detectedType")

        // Determine page count based on detected type
        val pageCount = detectedType.pageCount
        val size: Int =
            if (pageCount > 0) {
                // Use detected page count (subtract 1 because we read pages 0..size inclusive)
                pageCount - 1
            } else {
                // Fall back to platform-reported type if detection failed
                when (tech.type) {
                    UltralightTechnology.TYPE_ULTRALIGHT -> UltralightCard.ULTRALIGHT_SIZE
                    UltralightTechnology.TYPE_ULTRALIGHT_C -> UltralightCard.ULTRALIGHT_C_SIZE
                    else -> throw IllegalArgumentException("Unknown Ultralight type " + tech.type)
                }
            }

        var pageNumber = 0
        var pageBuffer = ByteArray(0)
        val pages = mutableListOf<UltralightPage>()
        var unauthorized = false
        while (pageNumber < size) {
            if (pageNumber % 4 == 0) {
                try {
                    pageBuffer = tech.readPages(pageNumber)
                    unauthorized = false
                } catch (e: Exception) {
                    // Transceive failure, maybe authentication problem
                    unauthorized = true
                }
            }

            if (!unauthorized) {
                pages.add(
                    UltralightPage.create(
                        pageNumber,
                        pageBuffer.copyOfRange(
                            (pageNumber % 4) * UltralightTechnology.PAGE_SIZE,
                            ((pageNumber % 4) + 1) * UltralightTechnology.PAGE_SIZE,
                        ),
                    ),
                )
            } else {
                pages.add(UltralightPage.unauthorized(pageNumber))
            }
            pageNumber++
        }

        return RawUltralightCard.create(tagId, Clock.System.now(), pages, tech.type)
    }

    /**
     * Detects the Ultralight card type using protocol commands.
     *
     * This uses GET_VERSION (0x60) and AUTH_1 (0x1a) commands to distinguish between:
     * - MF0ICU1 (Ultralight) - 16 pages
     * - MF0ICU2 (Ultralight C) - 44 pages
     * - EV1_MF0UL11 (Ultralight EV1 48 bytes) - 20 pages
     * - EV1_MF0UL21 (Ultralight EV1 128 bytes) - 41 pages
     * - NTAG213 - 45 pages
     * - NTAG215 - 135 pages
     * - NTAG216 - 231 pages
     *
     * Falls back to UNKNOWN if detection fails.
     */
    private fun detectCardType(tech: UltralightTechnology): UltralightCard.UltralightType =
        try {
            val protocol = UltralightProtocol(tech)
            val rawType = protocol.getCardType()
            rawType.parse()
        } catch (e: Exception) {
            println("UltralightCardReader: Card type detection failed, falling back to UNKNOWN: $e")
            UltralightCard.UltralightType.UNKNOWN
        }
}
