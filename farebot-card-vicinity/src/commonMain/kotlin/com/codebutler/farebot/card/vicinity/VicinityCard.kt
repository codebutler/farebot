/*
 * VicinityCard.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * NFC-V (ISO 15693) Vicinity card.
 *
 * These cards use a page-based memory structure similar to MIFARE Ultralight
 * but communicate via the ISO 15693 (Vicinity) protocol.
 */
@Serializable
data class VicinityCard(
    @Contextual override val tagId: ByteArray,
    override val scannedAt: Instant,
    val pages: List<VicinityPage>,
    @Contextual val sysInfo: ByteArray? = null,
    val isPartialRead: Boolean = false
) : Card() {

    override val cardType: CardType = CardType.Vicinity

    fun getPage(index: Int): VicinityPage = pages[index]

    /**
     * Read contiguous pages and concatenate their data.
     */
    fun readPages(startPage: Int, pageCount: Int): ByteArray {
        val result = mutableListOf<Byte>()
        for (i in startPage until startPage + pageCount) {
            result.addAll(pages[i].data.toList())
        }
        return result.toByteArray()
    }

    /**
     * Read arbitrary byte ranges across page boundaries.
     */
    fun readBytes(start: Int, len: Int): ByteArray {
        val pageSize = pages.firstOrNull()?.data?.size ?: 4
        val startPage = start / pageSize
        val startOffset = start % pageSize
        val endPage = (start + len - 1) / pageSize
        val allData = readPages(startPage, endPage - startPage + 1)
        return allData.copyOfRange(startOffset, startOffset + len)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree {
        val builder = FareBotUiTree.builder(stringResource)
        if (sysInfo != null) {
            builder.item()
                .title("System Info")
                .value(sysInfo)
        }
        val pagesBuilder = builder.item().title("Pages")
        for (page in pages) {
            val pageBuilder = pagesBuilder.item()
                .title("Page ${page.index}")
            if (page.isUnauthorized) {
                pageBuilder.value("Unauthorized")
            } else {
                pageBuilder.value(page.data)
            }
        }
        return builder.build()
    }

    companion object {
        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            pages: List<VicinityPage>,
            sysInfo: ByteArray? = null,
            isPartialRead: Boolean = false
        ): VicinityCard = VicinityCard(tagId, scannedAt, pages, sysInfo, isPartialRead)
    }
}
