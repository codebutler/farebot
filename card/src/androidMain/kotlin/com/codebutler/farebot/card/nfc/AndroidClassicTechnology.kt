/*
 * AndroidClassicTechnology.kt
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

import android.nfc.tech.MifareClassic

class AndroidClassicTechnology(
    private val mifareClassic: MifareClassic,
) : ClassicTechnology {
    override fun connect() {
        mifareClassic.connect()
    }

    override fun close() {
        mifareClassic.close()
    }

    override val isConnected: Boolean
        get() = mifareClassic.isConnected

    override val sectorCount: Int
        get() = mifareClassic.sectorCount

    override fun authenticateSectorWithKeyA(
        sectorIndex: Int,
        key: ByteArray,
    ): Boolean = mifareClassic.authenticateSectorWithKeyA(sectorIndex, key)

    override fun authenticateSectorWithKeyB(
        sectorIndex: Int,
        key: ByteArray,
    ): Boolean = mifareClassic.authenticateSectorWithKeyB(sectorIndex, key)

    override fun readBlock(blockIndex: Int): ByteArray = mifareClassic.readBlock(blockIndex)

    override fun sectorToBlock(sectorIndex: Int): Int = mifareClassic.sectorToBlock(sectorIndex)

    override fun getBlockCountInSector(sectorIndex: Int): Int = mifareClassic.getBlockCountInSector(sectorIndex)
}
