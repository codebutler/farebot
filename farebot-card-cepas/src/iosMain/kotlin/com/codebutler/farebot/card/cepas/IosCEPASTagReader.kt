/*
 * IosCEPASTagReader.kt
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

package com.codebutler.farebot.card.cepas

import com.codebutler.farebot.card.cepas.raw.RawCEPASCard
import com.codebutler.farebot.card.cepas.raw.RawCEPASHistory
import com.codebutler.farebot.card.cepas.raw.RawCEPASPurse
import com.codebutler.farebot.card.nfc.CardTransceiver
import kotlin.time.Clock

/**
 * iOS implementation of the CEPAS tag reader.
 *
 * CEPAS cards (EZ-Link Singapore) use ISO-DEP / ISO 7816 protocol. On iOS they
 * appear as NFCMiFareTag. The [CardTransceiver] wraps the iOS tag and the actual
 * protocol logic is shared via [CEPASProtocol] in commonMain.
 */
class IosCEPASTagReader(
    private val tagId: ByteArray,
    private val transceiver: CardTransceiver,
) {

    fun readTag(): RawCEPASCard {
        transceiver.connect()
        try {
            val purses = arrayOfNulls<RawCEPASPurse>(16)
            val histories = arrayOfNulls<RawCEPASHistory>(16)

            val protocol = CEPASProtocol(transceiver)

            for (purseId in purses.indices) {
                purses[purseId] = protocol.getPurse(purseId)
            }

            for (historyId in histories.indices) {
                val rawCEPASPurse = purses[historyId]!!
                if (rawCEPASPurse.isValid) {
                    val recordCount = rawCEPASPurse.logfileRecordCount().toString().toInt()
                    histories[historyId] = protocol.getHistory(historyId, recordCount)
                } else {
                    histories[historyId] = RawCEPASHistory.create(historyId, "Invalid Purse")
                }
            }

            return RawCEPASCard.create(
                tagId, Clock.System.now(),
                purses.toList() as List<RawCEPASPurse>,
                histories.toList() as List<RawCEPASHistory>,
            )
        } finally {
            if (transceiver.isConnected) {
                try {
                    transceiver.close()
                } catch (_: Exception) {
                }
            }
        }
    }
}
