/*
 * CEPASProtocol.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
 * Copyright (C) 2013-2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2018 Michael Farrell <micolous+git@gmail.com>
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

import com.codebutler.farebot.card.iso7816.ISO7816Exception
import com.codebutler.farebot.card.iso7816.ISO7816Protocol

internal class CEPASProtocol(
    private val protocol: ISO7816Protocol,
) {
    suspend fun getPurse(purseId: Int): ByteArray? =
        try {
            val result =
                protocol.sendRequest(
                    ISO7816Protocol.CLASS_90,
                    0x32.toByte(),
                    purseId.toByte(),
                    0.toByte(),
                    0.toByte(),
                )
            if (result.isEmpty()) null else result
        } catch (ex: ISO7816Exception) {
            println("[CEPAS] Failed to read purse $purseId: $ex")
            null
        }

    suspend fun getHistory(purseId: Int): ByteArray? {
        var historyBuff: ByteArray
        try {
            historyBuff =
                protocol.sendRequest(
                    ISO7816Protocol.CLASS_90,
                    0x32.toByte(),
                    purseId.toByte(),
                    0.toByte(),
                    0.toByte(),
                    byteArrayOf(0.toByte()),
                )
        } catch (ex: ISO7816Exception) {
            println("[CEPAS] Failed to read purse history: $ex")
            return null
        }

        try {
            val historyBuff2 =
                protocol.sendRequest(
                    ISO7816Protocol.CLASS_90,
                    0x32.toByte(),
                    purseId.toByte(),
                    0.toByte(),
                    0.toByte(),
                    byteArrayOf((historyBuff.size / 16).toByte()),
                )
            historyBuff = historyBuff + historyBuff2
        } catch (ex: ISO7816Exception) {
            println("[CEPAS] Failed to read 2nd purse history: $ex")
        }

        return historyBuff
    }
}
