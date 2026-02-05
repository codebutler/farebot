/*
 * CEPASProtocol.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2015-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
 * Copyright (C) 2012 tbonang <bonang@gmail.com>
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

package com.codebutler.farebot.card.cepas

import com.codebutler.farebot.card.cepas.raw.RawCEPASHistory
import com.codebutler.farebot.card.cepas.raw.RawCEPASPurse
import com.codebutler.farebot.card.nfc.CardTransceiver

internal class CEPASProtocol(private val mTransceiver: CardTransceiver) {

    fun getPurse(purseId: Int): RawCEPASPurse {
        try {
            sendSelectFile()
            val purseBuff = sendRequest(0x32.toByte(), purseId.toByte(), 0.toByte(), 0.toByte(), byteArrayOf(0.toByte()))
            return if (purseBuff != null) {
                RawCEPASPurse.create(purseId, purseBuff)
            } else {
                RawCEPASPurse.create(purseId, "No purse found")
            }
        } catch (ex: CEPASException) {
            return RawCEPASPurse.create(purseId, ex.message ?: "Unknown error")
        }
    }

    fun getHistory(purseId: Int, recordCount: Int): RawCEPASHistory {
        try {
            var fullHistoryBuff: ByteArray? = null
            val historyBuff = sendRequest(0x32.toByte(), purseId.toByte(), 0.toByte(), 1.toByte(),
                byteArrayOf(0.toByte(), (if (recordCount <= 15) recordCount * 16 else 15 * 16).toByte()))

            if (historyBuff != null) {
                if (recordCount > 15) {
                    var historyBuff2: ByteArray? = null
                    try {
                        historyBuff2 = sendRequest(0x32.toByte(), purseId.toByte(), 0.toByte(), 1.toByte(),
                            byteArrayOf(0x0F.toByte(), ((recordCount - 15) * 16).toByte()))
                    } catch (ex: CEPASException) {
                        // Error reading 2nd purse history
                    }
                    fullHistoryBuff = ByteArray(historyBuff.size + (historyBuff2?.size ?: 0))

                    historyBuff.copyInto(fullHistoryBuff, 0)
                    if (historyBuff2 != null) {
                        historyBuff2.copyInto(fullHistoryBuff, historyBuff.size)
                    }
                } else {
                    fullHistoryBuff = historyBuff
                }
            }

            return if (fullHistoryBuff != null) {
                RawCEPASHistory.create(purseId, fullHistoryBuff)
            } else {
                RawCEPASHistory.create(purseId, "No history found")
            }
        } catch (ex: CEPASException) {
            return RawCEPASHistory.create(purseId, ex.message ?: "Unknown error")
        }
    }

    private fun sendSelectFile(): ByteArray {
        return mTransceiver.transceive(CEPAS_SELECT_FILE_COMMAND)
    }

    @Throws(CEPASException::class)
    private fun sendRequest(command: Byte, p1: Byte, p2: Byte, lc: Byte, parameters: ByteArray): ByteArray? {
        val recvBuffer = mTransceiver.transceive(wrapMessage(command, p1, p2, lc, parameters))

        if (recvBuffer[recvBuffer.size - 2] != 0x90.toByte()) {
            if (recvBuffer[recvBuffer.size - 2] == 0x6b.toByte()) {
                throw CEPASException("File $p1 was an invalid file.")
            } else if (recvBuffer[recvBuffer.size - 2] == 0x67.toByte()) {
                throw CEPASException("Got invalid file size response.")
            }

            throw CEPASException("Got generic invalid response: "
                    + (recvBuffer[recvBuffer.size - 2].toInt() and 0xff).toString(16))
        }

        val output = recvBuffer.copyOfRange(0, recvBuffer.size - 2)

        val status = recvBuffer[recvBuffer.size - 1]
        return when (status) {
            OPERATION_OK -> output
            PERMISSION_DENIED -> throw CEPASException("Permission denied")
            else -> throw CEPASException("Unknown status code: " + (status.toInt() and 0xFF).toString(16))
        }
    }

    private fun wrapMessage(command: Byte, p1: Byte, p2: Byte, lc: Byte, parameters: ByteArray?): ByteArray {
        val paramSize = parameters?.size ?: 0
        val result = ByteArray(5 + paramSize)
        var offset = 0

        result[offset++] = 0x90.toByte() // CLA
        result[offset++] = command     // INS
        result[offset++] = p1          // P1
        result[offset++] = p2          // P2
        result[offset++] = lc          // Lc

        if (parameters != null) {
            parameters.copyInto(result, offset)
        }

        return result
    }

    companion object {
        private val CEPAS_SELECT_FILE_COMMAND = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x02.toByte(), 0x40.toByte(), 0x00.toByte()
        )

        /* Status codes */
        private const val OPERATION_OK: Byte = 0x00.toByte()
        private val PERMISSION_DENIED: Byte = 0x9D.toByte()
    }
}
