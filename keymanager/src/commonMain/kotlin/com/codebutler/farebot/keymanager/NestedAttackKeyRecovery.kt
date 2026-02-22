/*
 * NestedAttackKeyRecovery.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.keymanager

import com.codebutler.farebot.card.classic.ClassicKeyRecovery
import com.codebutler.farebot.card.nfc.pn533.PN533ClassicTechnology
import com.codebutler.farebot.keymanager.crypto1.NestedAttack
import com.codebutler.farebot.keymanager.pn533.PN533RawClassic

/**
 * [ClassicKeyRecovery] implementation using the MIFARE Classic nested attack.
 *
 * Given a known key for one sector, uses [NestedAttack] to recover unknown
 * keys for other sectors by exploiting the weak PRNG and Crypto1 cipher.
 */
class NestedAttackKeyRecovery : ClassicKeyRecovery {
    override suspend fun attemptRecovery(
        tech: PN533ClassicTechnology,
        sectorIndex: Int,
        knownKeys: Map<Int, Pair<ByteArray, Boolean>>,
        onProgress: ((String) -> Unit)?,
    ): Pair<ByteArray, Boolean>? {
        val knownEntry = knownKeys.entries.firstOrNull() ?: return null
        val (knownSector, knownKeyInfo) = knownEntry
        val (knownKeyBytes, knownIsKeyA) = knownKeyInfo
        val knownKey = keyBytesToLong(knownKeyBytes)
        val knownKeyType: Byte = if (knownIsKeyA) 0x60 else 0x61
        val knownBlock = tech.sectorToBlock(knownSector)
        val targetBlock = tech.sectorToBlock(sectorIndex)

        val rawClassic = PN533RawClassic(tech.rawPn533, tech.rawUid)
        val attack = NestedAttack(rawClassic, tech.uidAsUInt)

        val recoveredKey =
            attack.recoverKey(
                knownKeyType = knownKeyType,
                knownSectorBlock = knownBlock,
                knownKey = knownKey,
                targetKeyType = 0x60,
                targetBlock = targetBlock,
                onProgress = onProgress,
            )

        if (recoveredKey != null) {
            val keyBytes = longToKeyBytes(recoveredKey)
            // Try as Key A first
            val authA = tech.authenticateSectorWithKeyA(sectorIndex, keyBytes)
            if (authA) return Pair(keyBytes, true)

            // Try as Key B
            val authB = tech.authenticateSectorWithKeyB(sectorIndex, keyBytes)
            if (authB) return Pair(keyBytes, false)
        }

        return null
    }

    companion object {
        private fun keyBytesToLong(key: ByteArray): Long {
            var result = 0L
            for (i in 0 until minOf(6, key.size)) {
                result = (result shl 8) or (key[i].toLong() and 0xFF)
            }
            return result
        }

        private fun longToKeyBytes(key: Long): ByteArray =
            ByteArray(6) { i -> ((key ushr ((5 - i) * 8)) and 0xFF).toByte() }
    }
}
