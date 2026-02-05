/*
 * CEPASTagReader.kt
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

package com.codebutler.farebot.card.cepas

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.codebutler.farebot.card.TagReader
import com.codebutler.farebot.card.cepas.raw.RawCEPASCard
import com.codebutler.farebot.card.cepas.raw.RawCEPASHistory
import com.codebutler.farebot.card.cepas.raw.RawCEPASPurse
import com.codebutler.farebot.card.nfc.AndroidCardTransceiver
import com.codebutler.farebot.card.nfc.CardTransceiver
import com.codebutler.farebot.key.CardKeys
import kotlin.time.Clock

class CEPASTagReader(tagId: ByteArray, tag: Tag) :
    TagReader<CardTransceiver, RawCEPASCard, CardKeys>(tagId, tag, null) {

    override fun getTech(tag: Tag): CardTransceiver = AndroidCardTransceiver(IsoDep.get(tag))

    @Throws(Exception::class)
    override fun readTag(
        tagId: ByteArray,
        tag: Tag,
        tech: CardTransceiver,
        cardKeys: CardKeys?
    ): RawCEPASCard {
        val purses = arrayOfNulls<RawCEPASPurse>(16)
        val histories = arrayOfNulls<RawCEPASHistory>(16)

        val protocol = CEPASProtocol(tech)

        for (purseId in purses.indices) {
            purses[purseId] = protocol.getPurse(purseId)
        }

        for (historyId in histories.indices) {
            val rawCEPASPurse = purses[historyId]!!
            if (rawCEPASPurse.isValid) {
                val recordCount = Integer.parseInt(rawCEPASPurse.logfileRecordCount().toString())
                histories[historyId] = protocol.getHistory(historyId, recordCount)
            } else {
                histories[historyId] = RawCEPASHistory.create(historyId, "Invalid Purse")
            }
        }

        return RawCEPASCard.create(
            tag.id, Clock.System.now(),
            purses.toList().filterNotNull(),
            histories.toList().filterNotNull()
        )
    }
}
