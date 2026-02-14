/*
 * PCSCFeliCaTagAdapter.kt
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

import javax.smartcardio.CardChannel
import javax.smartcardio.CommandAPDU
import javax.smartcardio.ResponseAPDU

/**
 * PC/SC implementation of [FeliCaTagAdapter] for FeliCa (NFC-F) cards.
 *
 * Routes raw NFC-F command frames through the PC/SC reader using
 * the Encapsulate command (FF C2 00 01 for transparent exchange).
 *
 * Builds the same NFC-F command packets as [AndroidFeliCaTagAdapter],
 * but wraps them in PC/SC transport.
 */
class PCSCFeliCaTagAdapter(
    private val channel: CardChannel,
) : FeliCaTagAdapter {
    private var currentIdm: ByteArray? = null

    override fun getIDm(): ByteArray {
        val response =
            polling(FeliCaConstants.SYSTEMCODE_ANY)
                ?: throw Exception("Failed to poll for IDm")
        val idm = response.copyOfRange(2, 10)
        currentIdm = idm
        return idm
    }

    override fun getSystemCodes(): List<Int> {
        val idm = currentIdm ?: throw Exception("Must call getIDm() first")
        val cmd = buildFelicaCommand(FeliCaConstants.COMMAND_REQUEST_SYSTEMCODE, idm)
        val response = transceiveFelica(cmd) ?: return emptyList()
        if (response.size < 11) return emptyList()
        val count = response[10].toInt() and 0xff
        val codes = mutableListOf<Int>()
        for (i in 0 until count) {
            val offset = 11 + i * 2
            if (offset + 1 >= response.size) break
            val lo = response[offset].toInt() and 0xff
            val hi = response[offset + 1].toInt() and 0xff
            codes.add((hi shl 8) or lo)
        }
        return codes
    }

    override fun selectSystem(systemCode: Int): ByteArray? {
        val response = polling(systemCode) ?: return null
        if (response.size < 18) return null
        currentIdm = response.copyOfRange(2, 10)
        return response.copyOfRange(10, 18)
    }

    override fun getServiceCodes(): List<Int> {
        val idm = currentIdm ?: throw Exception("Must call getIDm() first")
        val serviceCodes = mutableListOf<Int>()
        var index = 1

        while (true) {
            val cmd =
                buildFelicaCommand(
                    FeliCaConstants.COMMAND_SEARCH_SERVICECODE,
                    idm,
                    (index and 0xff).toByte(),
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
                if (data[0] == 0xff.toByte() && data[1] == 0xff.toByte()) break
                val code = (data[0].toInt() and 0xff) or ((data[1].toInt() and 0xff) shl 8)
                serviceCodes.add(code)
            }
            index++
            if (index > 0xffff) break
        }
        return serviceCodes
    }

    override fun readBlock(
        serviceCode: Int,
        blockAddr: Byte,
    ): ByteArray? {
        val idm = currentIdm ?: throw Exception("Must call getIDm() first")
        val scLo = (serviceCode and 0xff).toByte()
        val scHi = (serviceCode shr 8).toByte()
        val cmd =
            buildFelicaCommand(
                FeliCaConstants.COMMAND_READ_WO_ENCRYPTION,
                idm,
                0x01,
                scLo,
                scHi,
                0x01,
                0x80.toByte(),
                blockAddr,
            )
        val response = transceiveFelica(cmd) ?: return null
        if (response.size < 12) return null
        val statusFlag1 = response[10].toInt() and 0xff
        if (statusFlag1 != 0x00) return null
        if (response.size < 14) return null
        val blockCount = response[12].toInt() and 0xff
        if (blockCount < 1 || response.size < 13 + blockCount * 16) return null
        return response.copyOfRange(13, 13 + 16)
    }

    private fun polling(systemCode: Int): ByteArray? {
        val cmd =
            buildFelicaCommand(
                FeliCaConstants.COMMAND_POLLING,
                byteArrayOf(),
                (systemCode shr 8).toByte(),
                (systemCode and 0xff).toByte(),
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

    /**
     * Send a raw NFC-F frame through the PC/SC reader.
     *
     * Uses the PC/SC Encapsulate command (CLA=FF, INS=C2, P1=00, P2=01)
     * which provides transparent access to the NFC-F contactless interface.
     */
    private fun transceiveFelica(felicaFrame: ByteArray): ByteArray? =
        try {
            val command = CommandAPDU(0xFF, 0xC2, 0x00, 0x01, felicaFrame, 256)
            val response: ResponseAPDU = channel.transmit(command)
            if (response.sW1 == 0x90 && response.sW2 == 0x00) {
                response.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
}
