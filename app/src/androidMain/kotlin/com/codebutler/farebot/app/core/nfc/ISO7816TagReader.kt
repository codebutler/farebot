/*
 * ISO7816TagReader.kt
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

package com.codebutler.farebot.app.core.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.TagReader
import com.codebutler.farebot.card.nfc.AndroidCardTransceiver
import com.codebutler.farebot.card.nfc.CardTransceiver
import com.codebutler.farebot.key.CardKeys
import com.codebutler.farebot.shared.nfc.ISO7816Dispatcher

/**
 * Tag reader for IsoDep tags that first tries ISO 7816 SELECT BY NAME
 * for known China and KSX6924 application identifiers, then falls back
 * to the DESFire protocol if no known ISO 7816 application is found.
 */
class ISO7816TagReader(
    tagId: ByteArray,
    tag: Tag,
) : TagReader<CardTransceiver, RawCard<*>, CardKeys>(tagId, tag, null) {
    override fun getTech(tag: Tag): CardTransceiver = AndroidCardTransceiver(IsoDep.get(tag))

    @Throws(Exception::class)
    override suspend fun readTag(
        tagId: ByteArray,
        tag: Tag,
        tech: CardTransceiver,
        cardKeys: CardKeys?,
        onProgress: (suspend (current: Int, total: Int) -> Unit)?,
    ): RawCard<*> = ISO7816Dispatcher.readCard(tagId, tech, onProgress)
}
