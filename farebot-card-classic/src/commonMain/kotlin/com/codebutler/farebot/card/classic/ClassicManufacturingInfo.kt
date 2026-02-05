/*
 * ClassicManufacturingInfo.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2018 Google
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic

/**
 * Manufacturing information extracted from block 0 of sector 0 of a MIFARE Classic card.
 */
data class ClassicManufacturingInfo(
    val manufacturer: Manufacturer,
    val sak: Int?,
    val atqa: Int?,
    val manufactureWeek: Int?,
    val manufactureYear: Int?
) {
    enum class Manufacturer {
        NXP,
        FUDAN,
        UNKNOWN
    }

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun parse(block0: ByteArray, tagId: ByteArray): ClassicManufacturingInfo? {
            if (block0.size < 16) return null

            // Detect Fudan Microelectronics FM11RF08
            val possibleFudan = block0.copyOfRange(8, 16)
            if (possibleFudan.contentEquals("bcdefghi".encodeToByteArray())) {
                return ClassicManufacturingInfo(
                    manufacturer = Manufacturer.FUDAN,
                    sak = block0[5].toInt() and 0xFF,
                    atqa = ((block0[6].toInt() and 0xFF) shl 8) or (block0[7].toInt() and 0xFF),
                    manufactureWeek = null,
                    manufactureYear = null
                )
            }

            // NXP: 7-byte UID starting with 0x04
            val isNxp = tagId.size == 7 && tagId[0] == 0x04.toByte()

            val sak: Int?
            val atqa: Int?
            if (isNxp) {
                sak = block0[7].toInt() and 0xFF
                atqa = ((block0[8].toInt() and 0xFF) shl 8) or (block0[9].toInt() and 0xFF)
            } else {
                sak = null
                atqa = null
            }

            // Manufacturing date from bytes 14-15 (BCD-encoded week and year)
            val weekRaw = block0[14].toInt() and 0xFF
            val yearRaw = block0[15].toInt() and 0xFF
            val validBcd = weekRaw in 0x01..0x53 &&
                (weekRaw and 0xF) in 0..9 &&
                (yearRaw and 0xF) in 0..9 &&
                yearRaw > 0 && yearRaw < 0x25

            val week: Int?
            val year: Int?
            if (validBcd) {
                week = weekRaw
                year = convertBcdToInt(yearRaw) + 2000
            } else {
                week = null
                year = null
            }

            return ClassicManufacturingInfo(
                manufacturer = if (isNxp) Manufacturer.NXP else Manufacturer.UNKNOWN,
                sak = sak,
                atqa = atqa,
                manufactureWeek = week,
                manufactureYear = year
            )
        }

        private fun convertBcdToInt(bcd: Int): Int {
            return ((bcd shr 4) and 0xF) * 10 + (bcd and 0xF)
        }
    }
}
