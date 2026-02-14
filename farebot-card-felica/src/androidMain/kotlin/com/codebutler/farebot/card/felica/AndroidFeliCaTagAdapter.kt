/*
 * AndroidFeliCaTagAdapter.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011 Kazzz
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

package com.codebutler.farebot.card.felica

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.NfcF
import java.io.IOException

/**
 * Android implementation of [FeliCaTagAdapter] using [NfcF] transceive.
 *
 * Builds raw NFC-F command packets inline and parses responses directly,
 * replacing the old FeliCaTag/FeliCaLibAndroid wrapper classes.
 */
class AndroidFeliCaTagAdapter(
    private val tag: Tag,
) : FeliCaTagAdapter {
    private var currentIdm: ByteArray? = null
    private var nfcF: NfcF? = null

    override fun getIDm(): ByteArray {
        // Poll with SYSTEMCODE_ANY to get IDm
        val response =
            polling(FeliCaConstants.SYSTEMCODE_ANY)
                ?: throw Exception("Failed to poll for IDm")
        // IDm is bytes 2..9 of the response
        val idm = response.copyOfRange(2, 10)
        currentIdm = idm
        return idm
    }

    override fun getSystemCodes(): List<Int> {
        val idm = currentIdm ?: throw Exception("Must call getIDm() first")
        // Build REQUEST_SYSTEMCODE command: length, command, IDm
        val cmd = buildCommand(FeliCaConstants.COMMAND_REQUEST_SYSTEMCODE, idm)
        val response = transceive(cmd) ?: return emptyList()
        if (response.size < 11) return emptyList()
        val count = response[10].toInt() and 0xff
        val codes = mutableListOf<Int>()
        for (i in 0 until count) {
            val offset = 11 + i * 2
            if (offset + 1 >= response.size) break
            // System codes come back in little-endian; convert to big-endian int
            val lo = response[offset].toInt() and 0xff
            val hi = response[offset + 1].toInt() and 0xff
            codes.add((hi shl 8) or lo)
        }
        return codes
    }

    override fun selectSystem(systemCode: Int): ByteArray? {
        val response = polling(systemCode) ?: return null
        if (response.size < 18) return null
        // Update current IDm from polling response
        currentIdm = response.copyOfRange(2, 10)
        // PMm is bytes 10..17
        return response.copyOfRange(10, 18)
    }

    override fun getServiceCodes(): List<Int> {
        val idm = currentIdm ?: throw Exception("Must call getIDm() first")
        val serviceCodes = mutableListOf<Int>()
        var index = 1 // 0 is root area, start from 1

        while (true) {
            // Build SEARCH_SERVICECODE command: IDm + index (little-endian 2 bytes)
            val cmd =
                buildCommand(
                    FeliCaConstants.COMMAND_SEARCH_SERVICECODE,
                    idm,
                    (index and 0xff).toByte(),
                    (index shr 8).toByte(),
                )
            val response = transceive(cmd)
            if (response == null || response.isEmpty() || response[1] != FeliCaConstants.RESPONSE_SEARCH_SERVICECODE) {
                break
            }
            val data = response.copyOfRange(10, response.size)
            if (data.size != 2 && data.size != 4) break
            if (data.size == 2) {
                if (data[0] == 0xff.toByte() && data[1] == 0xff.toByte()) break
                // Service code is little-endian
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
        // Service code bytes (little-endian)
        val scLo = (serviceCode and 0xff).toByte()
        val scHi = (serviceCode shr 8).toByte()
        // Build READ_WITHOUT_ENCRYPTION command
        val cmd =
            buildCommand(
                FeliCaConstants.COMMAND_READ_WO_ENCRYPTION,
                idm,
                0x01, // number of services
                scLo,
                scHi, // service code (little-endian)
                0x01, // number of blocks
                0x80.toByte(),
                blockAddr, // block list element (2-byte format)
            )
        val response = transceive(cmd) ?: return null
        // Check response: minimum length and status flags
        if (response.size < 12) return null
        val statusFlag1 = response[10].toInt() and 0xff
        if (statusFlag1 != 0x00) return null
        // Block data starts at offset 13 (after statusFlag1, statusFlag2, blockCount)
        if (response.size < 14) return null
        val blockCount = response[12].toInt() and 0xff
        if (blockCount < 1 || response.size < 13 + blockCount * 16) return null
        return response.copyOfRange(13, 13 + 16)
    }

    private fun polling(systemCode: Int): ByteArray? {
        // Build POLLING command: system code (big-endian), request code, time slot
        val cmd =
            buildCommand(
                FeliCaConstants.COMMAND_POLLING,
                byteArrayOf(), // no IDm for polling
                (systemCode shr 8).toByte(),
                (systemCode and 0xff).toByte(),
                0x01, // request system code
                0x00, // time slot
            )
        return transceive(cmd)
    }

    private fun buildCommand(
        commandCode: Byte,
        idm: ByteArray,
        vararg data: Byte,
    ): ByteArray {
        val length = 2 + idm.size + data.size // length byte + command byte + idm + data
        return byteArrayOf(length.toByte(), commandCode) + idm + data
    }

    private fun ensureConnected(): NfcF {
        nfcF?.let { if (it.isConnected) return it }
        val f = NfcF.get(tag) ?: throw Exception("Tag is not FeliCa (NFC-F)")
        f.connect()
        nfcF = f
        return f
    }

    fun close() {
        try {
            nfcF?.close()
        } catch (_: IOException) {
            // ignore
        }
        nfcF = null
    }

    private fun transceive(data: ByteArray): ByteArray? {
        try {
            val f = ensureConnected()
            return f.transceive(data)
        } catch (_: TagLostException) {
            return null
        } catch (e: IOException) {
            throw Exception("NFC transceive failed", e)
        }
    }
}
