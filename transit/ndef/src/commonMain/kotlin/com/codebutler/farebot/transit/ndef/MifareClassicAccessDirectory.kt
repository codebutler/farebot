/*
 * MifareClassicAccessDirectory.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.ndef

import com.codebutler.farebot.base.util.HashUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.ClassicSector
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector

class MifareClassicAccessDirectory(
    val aids: List<SectorIndex>,
) {
    fun contains(aid: Int): Boolean = aids.firstOrNull { it.aid == aid } != null

    fun getContiguous(aid: Int): List<Int> {
        val all = getAll(aid)
        val res = mutableListOf<Int>()
        for (el in all) {
            if (res.isNotEmpty() &&
                res.last() != el - 1 &&
                (res.last() != 0xf || el != 0x11)
            ) {
                break
            }
            res += el
        }
        return res
    }

    fun getAll(aid: Int): List<Int> = aids.filter { it.aid == aid }.map { it.sector }

    data class SectorIndex(
        val sector: Int,
        val aid: Int,
    )

    companion object {
        private fun parseAids(
            block: ByteArray,
            start: Int,
            skip: Int,
        ): List<SectorIndex> =
            (skip..7).map {
                SectorIndex(it + start, block.byteArrayToInt(it * 2, 2))
            }

        fun getMadVersion(sector0: ClassicSector): Int? {
            if (sector0 is UnauthorizedClassicSector) return null
            val dataClassicSector = sector0 as? DataClassicSector ?: return null

            try {
                val block3Data = dataClassicSector.getBlock(3).data
                if (block3Data.size < 10) return null
                val gpb = block3Data[9].toInt() and 0xff

                // We don't check keyA as it might be unknown if we read using keyB

                if ((gpb and 0x80 == 0) ||
                    // DA == 0
                    (gpb and 0x3c != 0) // RFU != 0
                ) {
                    return null
                }

                val madVersion = gpb and 0x3
                if (madVersion != 1 && madVersion != 2) {
                    return null
                }

                val block1Data = dataClassicSector.getBlock(1).data
                val infoByte = block1Data[1]

                if (infoByte == 0x10.toByte() || infoByte >= 0x28) {
                    return null
                }

                val storedCrc = block1Data[0].toInt() and 0xff

                val crc =
                    HashUtils.calculateCRC8NXP(
                        block1Data.sliceOffLen(1, 15),
                        dataClassicSector.getBlock(2).data,
                    )

                if (storedCrc != crc) {
                    return null
                }

                return madVersion
            } catch (e: Exception) {
                return null
            }
        }

        fun sector0Aids(sector0: DataClassicSector): List<SectorIndex> =
            parseAids(sector0.getBlock(1).data, 0, 1) +
                parseAids(sector0.getBlock(2).data, 8, 0)

        fun parse(card: ClassicCard): MifareClassicAccessDirectory? {
            try {
                val sector0 = card.getSector(0)
                val madVersion = getMadVersion(sector0) ?: return null
                val dataClassicSector0 = sector0 as? DataClassicSector ?: return null

                if (madVersion == 2 && card.sectors.size <= 0x10) {
                    return null
                }

                val infoByte = dataClassicSector0.getBlock(1).data[1]

                if (infoByte >= card.sectors.size) {
                    return null
                }

                val aids = sector0Aids(dataClassicSector0)

                if (madVersion == 1) {
                    return MifareClassicAccessDirectory(aids)
                }

                val sector16 = card.getSector(0x10) as? DataClassicSector ?: return null
                val gpb2 = sector16.getBlock(3).data[9].toInt() and 0xff

                if (gpb2 != 0) {
                    return null
                }

                val infoByte2 = sector16.getBlock(0).data[1]

                if (infoByte2 == 0x10.toByte() || infoByte2 >= card.sectors.size) {
                    return null
                }

                val crc2 =
                    HashUtils.calculateCRC8NXP(
                        sector16.getBlock(0).data.sliceOffLen(1, 15),
                        sector16.getBlock(1).data,
                        sector16.getBlock(2).data,
                    )

                val storedCrc2 = sector16.getBlock(0).data[0].toInt() and 0xff

                if (storedCrc2 != crc2) {
                    return null
                }

                val aids2 =
                    parseAids(sector16.getBlock(0).data, 16, 1) +
                        parseAids(sector16.getBlock(1).data, 24, 0) +
                        parseAids(sector16.getBlock(2).data, 32, 0)

                return MifareClassicAccessDirectory(aids + aids2)
            } catch (e: Exception) {
                return null
            }
        }

        fun sector0Contains(
            sector0: ClassicSector,
            aid: Int,
        ): Boolean {
            getMadVersion(sector0) ?: return false
            val dataClassicSector = sector0 as? DataClassicSector ?: return false
            return sector0Aids(dataClassicSector).firstOrNull { it.aid == aid } != null
        }

        const val NFC_AID = 0x3e1
    }
}
