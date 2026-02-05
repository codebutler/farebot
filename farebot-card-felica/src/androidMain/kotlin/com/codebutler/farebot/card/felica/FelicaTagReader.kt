/*
 * FelicaTagReader.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.card.felica

import android.nfc.Tag
import android.nfc.tech.NfcF
import com.codebutler.farebot.card.TagReader
import com.codebutler.farebot.card.felica.raw.RawFelicaCard
import com.codebutler.farebot.card.nfc.AndroidNfcFTechnology
import com.codebutler.farebot.card.nfc.NfcFTechnology
import com.codebutler.farebot.key.CardKeys

class FelicaTagReader(tagId: ByteArray, tag: Tag) :
    TagReader<NfcFTechnology, RawFelicaCard, CardKeys>(tagId, tag, null) {

    override fun getTech(tag: Tag): NfcFTechnology = AndroidNfcFTechnology(NfcF.get(tag))

    @Throws(Exception::class)
    override fun readTag(
        tagId: ByteArray,
        tag: Tag,
        tech: NfcFTechnology,
        cardKeys: CardKeys?
    ): RawFelicaCard {
        val adapter = AndroidFeliCaTagAdapter(tag)
        return FeliCaReader.readTag(tagId, adapter)
    }
}
