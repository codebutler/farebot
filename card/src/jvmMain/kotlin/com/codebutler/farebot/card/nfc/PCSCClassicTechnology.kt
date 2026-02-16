/*
 * PCSCClassicTechnology.kt
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

package com.codebutler.farebot.card.nfc

import javax.smartcardio.CardChannel
import javax.smartcardio.CommandAPDU
import javax.smartcardio.ResponseAPDU

/**
 * PC/SC implementation of [ClassicTechnology] for MIFARE Classic cards.
 *
 * Uses PC/SC pseudo-APDUs for key loading, authentication, and block reading:
 * - LOAD KEY: FF 82 00 {slot} 06 {key}
 * - AUTHENTICATE: FF 86 00 00 05 01 00 {block} {keyType} {slot}
 * - READ BINARY: FF B0 00 {block} 10
 */
class PCSCClassicTechnology(
    private val channel: CardChannel,
    private val info: PCSCCardInfo,
) : ClassicTechnology {
    private var connected = true

    override fun connect() {
        connected = true
    }

    override fun close() {
        connected = false
    }

    override val isConnected: Boolean get() = connected

    override val sectorCount: Int get() = info.classicSectorCount

    override suspend fun authenticateSectorWithKeyA(
        sectorIndex: Int,
        key: ByteArray,
    ): Boolean = authenticate(sectorIndex, key, KEY_TYPE_A)

    override suspend fun authenticateSectorWithKeyB(
        sectorIndex: Int,
        key: ByteArray,
    ): Boolean = authenticate(sectorIndex, key, KEY_TYPE_B)

    override suspend fun readBlock(blockIndex: Int): ByteArray {
        // READ BINARY: FF B0 00 {block} 10
        val command = CommandAPDU(0xFF, 0xB0, 0x00, blockIndex, 16)
        val response = channel.transmit(command)
        if (response.sW1 != 0x90 || response.sW2 != 0x00) {
            throw Exception("Read block $blockIndex failed: SW=${sw(response)}")
        }
        return response.data
    }

    override fun sectorToBlock(sectorIndex: Int): Int =
        if (sectorIndex < 32) {
            sectorIndex * 4
        } else {
            // Large sectors (32+) have 16 blocks each
            32 * 4 + (sectorIndex - 32) * 16
        }

    override fun getBlockCountInSector(sectorIndex: Int): Int = if (sectorIndex < 32) 4 else 16

    private fun authenticate(
        sectorIndex: Int,
        key: ByteArray,
        keyType: Byte,
    ): Boolean {
        // Load key into volatile key slot 0
        // LOAD KEY: FF 82 00 {slot} 06 {key[6]}
        val loadKeyCommand =
            CommandAPDU(
                0xFF,
                0x82,
                0x00,
                KEY_SLOT,
                key,
            )
        val loadResponse = channel.transmit(loadKeyCommand)
        if (loadResponse.sW1 != 0x90 || loadResponse.sW2 != 0x00) {
            return false
        }

        // Authenticate: FF 86 00 00 05 {01 00 block keyType slot}
        val firstBlock = sectorToBlock(sectorIndex)
        val authData =
            byteArrayOf(
                0x01, // Version
                0x00, // Reserved
                firstBlock.toByte(),
                keyType,
                KEY_SLOT.toByte(),
            )
        val authCommand = CommandAPDU(0xFF, 0x86, 0x00, 0x00, authData)
        val authResponse = channel.transmit(authCommand)
        return authResponse.sW1 == 0x90 && authResponse.sW2 == 0x00
    }

    companion object {
        private const val KEY_TYPE_A: Byte = 0x60
        private const val KEY_TYPE_B: Byte = 0x61
        private const val KEY_SLOT = 0x00

        private fun sw(r: ResponseAPDU): String = "%02X%02X".format(r.sW1, r.sW2)
    }
}
