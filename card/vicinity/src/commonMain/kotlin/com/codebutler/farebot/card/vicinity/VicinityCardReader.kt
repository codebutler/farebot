/*
 * VicinityCardReader.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.card.vicinity

import com.codebutler.farebot.card.nfc.VicinityTechnology
import com.codebutler.farebot.card.vicinity.raw.RawVicinityCard
import kotlin.time.Clock

/**
 * Shared card-reading algorithm for NFC-V (ISO 15693) Vicinity cards.
 *
 * Uses [VicinityTechnology] to communicate with the tag, producing a
 * [RawVicinityCard] with all readable pages.
 *
 * Reference: https://www.ti.com/lit/an/sloa141/sloa141.pdf
 */
object VicinityCardReader {
    private const val MAX_PAGES = 255

    /**
     * Read an NFC-V tag using the provided technology interface.
     *
     * @param tagId The tag's identifier
     * @param tech The NFC-V technology interface for communication
     * @return A [RawVicinityCard] containing the read data
     */
    suspend fun readCard(
        tagId: ByteArray,
        tech: VicinityTechnology,
        onProgress: (suspend (current: Int, total: Int) -> Unit)? = null,
    ): RawVicinityCard {
        val uid = tech.uid

        // Try to read system info (command 0x2b)
        val sysInfo =
            try {
                val sysInfoRsp = tech.transceive(byteArrayOf(0x22, 0x2b.toByte()) + uid)
                if (sysInfoRsp.isNotEmpty() && sysInfoRsp[0] == 0.toByte()) {
                    // Skip status byte (first byte)
                    sysInfoRsp.copyOfRange(1, sysInfoRsp.size)
                } else {
                    null
                }
            } catch (e: Exception) {
                println("[VicinityCardReader] Failed to read system info: $e")
                null
            }

        var isPartialRead = false

        // Read pages sequentially
        val pages = mutableListOf<VicinityPage>()
        var pageIndex = 0

        while (pageIndex <= MAX_PAGES) {
            // Read single block command (0x20) with addressed mode (flag 0x22)
            val readCmd = byteArrayOf(0x22, 0x20) + uid + byteArrayOf(pageIndex.toByte())

            val response: ByteArray
            try {
                response = tech.transceive(readCmd)
            } catch (e: Exception) {
                // Card lost or other error - mark as partial read and stop
                isPartialRead = true
                break
            }

            // Check response: must not be empty and status byte must be 0
            if (response.isEmpty() || response[0] != 0.toByte()) {
                // End of readable memory
                break
            }

            // Skip status byte and extract page data
            val pageData = response.copyOfRange(1, response.size)
            pages.add(VicinityPage.create(pageIndex, pageData))

            pageIndex++
        }

        return RawVicinityCard.create(
            tagId = tagId,
            scannedAt = Clock.System.now(),
            pages = pages,
            sysInfo = sysInfo,
            isPartialRead = isPartialRead,
        )
    }
}
