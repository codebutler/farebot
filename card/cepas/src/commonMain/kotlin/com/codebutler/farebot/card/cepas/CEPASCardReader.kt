/*
 * CEPASCardReader.kt
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

import com.codebutler.farebot.card.cepas.raw.RawCEPASCard
import com.codebutler.farebot.card.cepas.raw.RawCEPASHistory
import com.codebutler.farebot.card.cepas.raw.RawCEPASPurse
import com.codebutler.farebot.card.iso7816.ISO7816Protocol
import com.codebutler.farebot.card.nfc.CardTransceiver
import kotlin.time.Clock

object CEPASCardReader {
    @Throws(Exception::class)
    fun readCard(
        tagId: ByteArray,
        tech: CardTransceiver,
    ): RawCEPASCard {
        val purses = arrayOfNulls<RawCEPASPurse>(16)
        val histories = arrayOfNulls<RawCEPASHistory>(16)

        val protocol = CEPASProtocol(ISO7816Protocol(tech))

        for (purseId in purses.indices) {
            val purseData = protocol.getPurse(purseId)
            purses[purseId] = if (purseData != null) {
                RawCEPASPurse.create(purseId, purseData)
            } else {
                RawCEPASPurse.create(purseId, "No purse found")
            }
        }

        for (historyId in histories.indices) {
            val rawCEPASPurse = purses[historyId]!!
            if (rawCEPASPurse.isValid) {
                val historyData = protocol.getHistory(historyId)
                histories[historyId] = if (historyData != null) {
                    RawCEPASHistory.create(historyId, historyData)
                } else {
                    RawCEPASHistory.create(historyId, "No history found")
                }
            } else {
                histories[historyId] = RawCEPASHistory.create(historyId, "Invalid Purse")
            }
        }

        return RawCEPASCard.create(
            tagId,
            Clock.System.now(),
            purses.toList().filterNotNull(),
            histories.toList().filterNotNull(),
        )
    }
}
