/*
 * ClassicStaticKeys.kt
 *
 * Copyright 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic.key

/**
 * Well-known static keys for MIFARE Classic transit systems.
 *
 * These are publicly documented keys that can be used without user-provided key files.
 * They enable reading cards from common transit systems out of the box.
 */
object ClassicStaticKeys {

    /** Factory default key (all 0xFF) */
    val KEY_DEFAULT = hexToBytes("FFFFFFFFFFFF")

    /** Zero key (all 0x00) */
    val KEY_ZERO = hexToBytes("000000000000")

    /** MAD key (MIFARE Application Directory) */
    val KEY_MAD = hexToBytes("A0A1A2A3A4A5")

    /** NFC Forum key (NDEF) */
    val KEY_NFC_FORUM = hexToBytes("D3F7D3F7D3F7")

    /**
     * Returns a list of well-known keys to try when authenticating a sector.
     * These are tried in addition to any user-provided keys.
     */
    fun getWellKnownKeys(): List<ByteArray> = listOf(
        KEY_DEFAULT,
        KEY_ZERO,
        KEY_MAD,
        KEY_NFC_FORUM
    )

    /**
     * Creates a ClassicCardKeys with default keys for all sectors.
     * Useful as a fallback when no system-specific keys are available.
     *
     * @param sectorCount Number of sectors on the card (typically 16 for 1K, 40 for 4K)
     */
    fun defaultKeysForSectorCount(sectorCount: Int): ClassicCardKeys {
        val keys = (0 until sectorCount).map {
            ClassicSectorKey.create(KEY_DEFAULT, KEY_DEFAULT)
        }
        return ClassicCardKeys(
            cardType = com.codebutler.farebot.card.CardType.MifareClassic,
            keys = keys
        )
    }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }
}
