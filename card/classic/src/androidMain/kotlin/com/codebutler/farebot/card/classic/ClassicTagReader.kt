/*
 * ClassicTagReader.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.card.classic

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import com.codebutler.farebot.card.TagReader
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.card.nfc.AndroidClassicTechnology
import com.codebutler.farebot.card.nfc.ClassicTechnology

class ClassicTagReader(
    tagId: ByteArray,
    tag: Tag,
    cardKeys: ClassicCardKeys?,
) : TagReader<ClassicTechnology, RawClassicCard, ClassicCardKeys>(tagId, tag, cardKeys) {
    override fun getTech(tag: Tag): ClassicTechnology = AndroidClassicTechnology(MifareClassic.get(tag))

    @Throws(Exception::class)
    override suspend fun readTag(
        tagId: ByteArray,
        tag: Tag,
        tech: ClassicTechnology,
        cardKeys: ClassicCardKeys?,
    ): RawClassicCard = ClassicCardReader.readCard(tagId, tech, cardKeys)
}
