/*
 * VicinityTagReader.kt
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

package com.codebutler.farebot.card.vicinity

import android.nfc.Tag
import android.nfc.tech.NfcV
import com.codebutler.farebot.card.TagReader
import com.codebutler.farebot.card.nfc.AndroidVicinityTechnology
import com.codebutler.farebot.card.nfc.VicinityTechnology
import com.codebutler.farebot.card.vicinity.raw.RawVicinityCard
import com.codebutler.farebot.key.CardKeys

/**
 * Android TagReader for NFC-V (ISO 15693 Vicinity) tags.
 *
 * Wraps Android's [NfcV] technology and delegates to [VicinityCardReader]
 * for the common reading logic.
 */
class VicinityTagReader(
    tagId: ByteArray,
    tag: Tag,
) : TagReader<VicinityTechnology, RawVicinityCard, CardKeys>(tagId, tag, null) {
    override fun getTech(tag: Tag): VicinityTechnology = AndroidVicinityTechnology(NfcV.get(tag))

    @Throws(Exception::class)
    override suspend fun readTag(
        tagId: ByteArray,
        tag: Tag,
        tech: VicinityTechnology,
        cardKeys: CardKeys?,
    ): RawVicinityCard = VicinityCardReader.readCard(tagId, tech)
}
