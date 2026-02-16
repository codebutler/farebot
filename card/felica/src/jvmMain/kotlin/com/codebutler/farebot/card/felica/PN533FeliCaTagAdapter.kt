/*
 * PN533FeliCaTagAdapter.kt
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

package com.codebutler.farebot.card.felica

import com.codebutler.farebot.card.nfc.pn533.PN533

/**
 * PN533 implementation of [FeliCaTagAdapter] for FeliCa (NFC-F) cards.
 *
 * Uses [PN533.inCommunicateThru] to send raw NFC-F frames directly,
 * bypassing PC/SC encapsulation. Builds the same FeliCa command packets
 * as [PCSCFeliCaTagAdapter] but routes them through the PN533 USB transport.
 */
class PN533FeliCaTagAdapter(
    private val pn533: PN533,
    initialIdm: ByteArray,
) : FeliCaTagAdapter {
    private var currentIdm: ByteArray = initialIdm

    override fun getIDm(): ByteArray = currentIdm

    override suspend fun getSystemCodes(): List<Int> {
        val cmd = buildFelicaCommand(FeliCaConstants.COMMAND_REQUEST_SYSTEMCODE, currentIdm)
        val response = transceiveFelica(cmd) ?: return emptyList()
        if (response.size < 11) return emptyList()
        val count = response[10].toInt() and 0xFF
        val codes = mutableListOf<Int>()
        for (i in 0 until count) {
            val offset = 11 + i * 2
            if (offset + 1 >= response.size) break
            val lo = response[offset].toInt() and 0xFF
            val hi = response[offset + 1].toInt() and 0xFF
            codes.add((hi shl 8) or lo)
        }
        return codes
    }

    override suspend fun selectSystem(systemCode: Int): ByteArray? {
        val response = polling(systemCode) ?: return null
        if (response.size < 18) return null
        currentIdm = response.copyOfRange(2, 10)
        return response.copyOfRange(10, 18)
    }

    override suspend fun getServiceCodes(): List<Int> {
        val serviceCodes = mutableListOf<Int>()
        var index = 1

        while (true) {
            val cmd =
                buildFelicaCommand(
                    FeliCaConstants.COMMAND_SEARCH_SERVICECODE,
                    currentIdm,
                    (index and 0xFF).toByte(),
                    (index shr 8).toByte(),
                )
            val response = transceiveFelica(cmd)
            if (response == null ||
                response.isEmpty() ||
                response[1] != FeliCaConstants.RESPONSE_SEARCH_SERVICECODE
            ) {
                break
            }
            val data = response.copyOfRange(10, response.size)
            if (data.size != 2 && data.size != 4) break
            if (data.size == 2) {
                if (data[0] == 0xFF.toByte() && data[1] == 0xFF.toByte()) break
                val code = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
                serviceCodes.add(code)
            }
            index++
            if (index > 0xFFFF) break
        }
        return serviceCodes
    }

    override suspend fun readBlock(
        serviceCode: Int,
        blockAddr: Byte,
    ): ByteArray? {
        val scLo = (serviceCode and 0xFF).toByte()
        val scHi = (serviceCode shr 8).toByte()
        val cmd =
            buildFelicaCommand(
                FeliCaConstants.COMMAND_READ_WO_ENCRYPTION,
                currentIdm,
                0x01,
                scLo,
                scHi,
                0x01,
                0x80.toByte(),
                blockAddr,
            )
        val response = transceiveFelica(cmd) ?: return null
        if (response.size < 12) return null
        val statusFlag1 = response[10].toInt() and 0xFF
        if (statusFlag1 != 0x00) return null
        if (response.size < 14) return null
        val blockCount = response[12].toInt() and 0xFF
        if (blockCount < 1 || response.size < 13 + blockCount * 16) return null
        return response.copyOfRange(13, 13 + 16)
    }

    private suspend fun polling(systemCode: Int): ByteArray? {
        val cmd =
            buildFelicaCommand(
                FeliCaConstants.COMMAND_POLLING,
                byteArrayOf(),
                (systemCode shr 8).toByte(),
                (systemCode and 0xFF).toByte(),
                0x01,
                0x00,
            )
        return transceiveFelica(cmd)
    }

    private fun buildFelicaCommand(
        commandCode: Byte,
        idm: ByteArray,
        vararg data: Byte,
    ): ByteArray {
        val length = 2 + idm.size + data.size
        return byteArrayOf(length.toByte(), commandCode) + idm + data
    }

    private suspend fun transceiveFelica(felicaFrame: ByteArray): ByteArray? =
        try {
            pn533.inCommunicateThru(felicaFrame)
        } catch (_: Exception) {
            null
        }
}
