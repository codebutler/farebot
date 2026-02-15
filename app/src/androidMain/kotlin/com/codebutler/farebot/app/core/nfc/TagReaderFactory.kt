/*
 * TagReaderFactory.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
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
import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.card.TagReader
import com.codebutler.farebot.card.cepas.CEPASTagReader
import com.codebutler.farebot.card.classic.ClassicTagReader
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.card.felica.FelicaTagReader
import com.codebutler.farebot.card.ultralight.UltralightTagReader
import com.codebutler.farebot.card.vicinity.VicinityTagReader
import com.codebutler.farebot.key.CardKeys

class TagReaderFactory {
    fun getTagReader(
        tagId: ByteArray,
        tag: Tag,
        cardKeys: CardKeys?,
    ): TagReader<*, *, *> =
        when {
            "android.nfc.tech.NfcB" in tag.techList -> CEPASTagReader(tagId, tag)
            "android.nfc.tech.IsoDep" in tag.techList -> ISO7816TagReader(tagId, tag)
            "android.nfc.tech.NfcF" in tag.techList -> FelicaTagReader(tagId, tag)
            "android.nfc.tech.MifareClassic" in tag.techList ->
                ClassicTagReader(
                    tagId,
                    tag,
                    cardKeys as ClassicCardKeys?,
                )
            "android.nfc.tech.MifareUltralight" in tag.techList -> UltralightTagReader(tagId, tag)
            "android.nfc.tech.NfcV" in tag.techList -> VicinityTagReader(tagId, tag)
            else -> throw UnsupportedTagException(tag.techList, ByteUtils.getHexString(tag.id))
        }
}
