/*
 * UltralightTagReader.kt
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

import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import com.codebutler.farebot.card.TagReader
import com.codebutler.farebot.card.nfc.AndroidUltralightTechnology
import com.codebutler.farebot.card.nfc.UltralightTechnology
import com.codebutler.farebot.card.ultralight.raw.RawUltralightCard
import com.codebutler.farebot.key.CardKeys
import java.util.ArrayList
import kotlin.time.Clock

class UltralightTagReader(
    tagId: ByteArray,
    tag: Tag,
) : TagReader<UltralightTechnology, RawUltralightCard, CardKeys>(tagId, tag, null) {
    override fun getTech(tag: Tag): UltralightTechnology = AndroidUltralightTechnology(MifareUltralight.get(tag))

    @Throws(Exception::class)
    override fun readTag(
        tagId: ByteArray,
        tag: Tag,
        tech: UltralightTechnology,
        cardKeys: CardKeys?,
    ): RawUltralightCard {
        val size: Int =
            when (tech.type) {
                UltralightTechnology.TYPE_ULTRALIGHT -> UltralightCard.ULTRALIGHT_SIZE
                UltralightTechnology.TYPE_ULTRALIGHT_C -> UltralightCard.ULTRALIGHT_C_SIZE
                // unknown
                else -> throw IllegalArgumentException("Unknown Ultralight type " + tech.type)
            }

        // Now iterate through the pages and grab all the datas
        var pageNumber = 0
        var pageBuffer = ByteArray(0)
        val pages = ArrayList<UltralightPage>()
        while (pageNumber <= size) {
            if (pageNumber % 4 == 0) {
                // Lets make a new buffer of data. (16 bytes = 4 pages * 4 bytes)
                pageBuffer = tech.readPages(pageNumber)
            }

            // Now lets stuff this into some pages.
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

        // Now we have pages to stuff in the card.
        return RawUltralightCard.create(
            tagId,
            Clock.System.now(),
            pages,
            tech.type,
        )
    }
}
