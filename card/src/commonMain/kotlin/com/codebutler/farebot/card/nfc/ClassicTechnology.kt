/*
 * ClassicTechnology.kt
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

interface ClassicTechnology : NfcTechnology {
    val sectorCount: Int

    fun authenticateSectorWithKeyA(
        sectorIndex: Int,
        key: ByteArray,
    ): Boolean

    fun authenticateSectorWithKeyB(
        sectorIndex: Int,
        key: ByteArray,
    ): Boolean

    fun readBlock(blockIndex: Int): ByteArray

    fun sectorToBlock(sectorIndex: Int): Int

    fun getBlockCountInSector(sectorIndex: Int): Int

    companion object {
        val KEY_DEFAULT: ByteArray =
            byteArrayOf(
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
            )
    }
}
