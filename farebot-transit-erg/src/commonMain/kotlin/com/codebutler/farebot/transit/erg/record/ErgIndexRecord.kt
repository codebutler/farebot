/*
 * ErgIndexRecord.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.transit.erg.record

import com.codebutler.farebot.card.classic.DataClassicSector

/**
 * Manages card block allocation index for ERG cards.
 * Maps block numbers to record types (0x03 for balance, 0x14-0x1d for purse records).
 */
data class ErgIndexRecord(
    val version: Int,
    val version2: Int,
    private val allocations: Map<Int, Int>
) {

    fun readRecord(sectorNum: Int, blockNum: Int, data: ByteArray): ErgRecord? {
        val block = sectorNum * 3 + blockNum
        val type = allocations[block] ?: 0
        val factory = FACTORIES[type] ?: return null
        return factory(data)
    }

    companion object {
        private val FACTORIES: Map<Int, (ByteArray) -> ErgRecord?> = mapOf(
            0x03 to ErgBalanceRecord.Companion::recordFromBytes,
            0x14 to ErgPurseRecord.Companion::recordFromBytes,
            0x15 to ErgPurseRecord.Companion::recordFromBytes,
            0x16 to ErgPurseRecord.Companion::recordFromBytes,
            0x17 to ErgPurseRecord.Companion::recordFromBytes,
            0x18 to ErgPurseRecord.Companion::recordFromBytes,
            0x19 to ErgPurseRecord.Companion::recordFromBytes,
            0x1a to ErgPurseRecord.Companion::recordFromBytes,
            0x1b to ErgPurseRecord.Companion::recordFromBytes,
            0x1c to ErgPurseRecord.Companion::recordFromBytes,
            0x1d to ErgPurseRecord.Companion::recordFromBytes
        )

        fun recordFromSector(sector: DataClassicSector): ErgIndexRecord {
            return recordFromBytes(
                sector.getBlock(0).data,
                sector.getBlock(1).data,
                sector.getBlock(2).data
            )
        }

        fun recordFromBytes(
            block0: ByteArray,
            block1: ByteArray,
            block2: ByteArray
        ): ErgIndexRecord {
            val version = ErgRecord.byteArrayToInt(block0, 1, 2)
            val allocations = mutableMapOf<Int, Int>()

            var offset = 6
            for (x in 3..15) {
                allocations[offset + x] = block0[x].toInt() and 0xFF
            }

            offset += 16
            repeat(16) {
                allocations[offset + it] = block1[it].toInt() and 0xFF
            }

            offset += 16
            repeat(10) {
                allocations[offset + it] = block2[it].toInt() and 0xFF
            }

            val version2 = ErgRecord.byteArrayToInt(block2, 11, 2)
            return ErgIndexRecord(version, version2, allocations)
        }
    }
}
