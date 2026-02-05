/*
 * ClassicCardKeys.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.card.classic.key

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.key.CardKeys
import kotlinx.serialization.Serializable

@Serializable
data class ClassicCardKeys(
    private val cardType: CardType,
    val keys: List<ClassicSectorKey>
) : CardKeys {

    override fun cardType(): CardType = cardType

    /**
     * Gets the key for a particular sector on the card.
     *
     * @param sectorNumber The sector number to retrieve the key for
     * @return A ClassicSectorKey for that sector, or null if there is no known key or the value is
     * out of range.
     */
    fun keyForSector(sectorNumber: Int): ClassicSectorKey? {
        if (sectorNumber >= keys.size) {
            return null
        }
        return keys[sectorNumber]
    }

    companion object {
        /**
         * Mifare Classic uses 48-bit keys.
         */
        private const val KEY_LEN = 6

        /**
         * Reads keys from a binary bin dump created by proxmark3.
         */
        fun fromProxmark3(keysDump: ByteArray): ClassicCardKeys {
            val keys = mutableListOf<ClassicSectorKey>()
            val numSectors = keysDump.size / KEY_LEN / 2
            for (i in 0 until numSectors) {
                val keyAOffset = i * KEY_LEN
                val keyBOffset = (i * KEY_LEN) + (KEY_LEN * numSectors)
                keys.add(ClassicSectorKey.create(readKey(keysDump, keyAOffset), readKey(keysDump, keyBOffset)))
            }
            return create(keys)
        }

        private fun create(keys: List<ClassicSectorKey>): ClassicCardKeys =
            ClassicCardKeys(CardType.MifareClassic, keys)

        private fun readKey(data: ByteArray, offset: Int): ByteArray =
            data.copyOfRange(offset, offset + KEY_LEN)
    }
}
