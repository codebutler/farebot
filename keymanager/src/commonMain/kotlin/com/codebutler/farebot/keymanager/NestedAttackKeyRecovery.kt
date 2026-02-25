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
import com.codebutler.farebot.keymanager.crypto1.HardnestedAttack
import com.codebutler.farebot.keymanager.crypto1.NestedAttack
import com.codebutler.farebot.keymanager.pn533.PN533RawClassic

/**
 * [ClassicKeyRecovery] implementation using MIFARE Classic nested and hardnested attacks.
 *
 * Given a known key for one sector, first attempts the standard [NestedAttack]
 * (exploiting the weak PRNG). If the card has a true RNG (MIFARE Classic EV1+),
 * falls back to the [HardnestedAttack] which uses statistical analysis of
 * encrypted parity bits.
 */
class NestedAttackKeyRecovery : ClassicKeyRecovery {
    /** Once true RNG is detected, skip standard nested attack for subsequent sectors. */
    private var cardHasTrueRng = false

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

        // Try standard nested attack first (unless we already know it's a true RNG card)
        if (!cardHasTrueRng) {
            val attack = NestedAttack(rawClassic, tech.uidAsUInt)
            when (
                val result =
                    attack.recoverKey(
                        knownKeyType = knownKeyType,
                        knownSectorBlock = knownBlock,
                        knownKey = knownKey,
                        targetKeyType = 0x60,
                        targetBlock = targetBlock,
                        onProgress = onProgress,
                    )
            ) {
                is NestedAttack.RecoverKeyResult.Success -> {
                    rawClassic.hardReselectCard()
                    return verifyRecoveredKey(tech, sectorIndex, result.key)
                }
                is NestedAttack.RecoverKeyResult.TrueRng -> {
                    cardHasTrueRng = true
                    onProgress?.invoke("Card has true RNG — switching to hardnested attack")
                }
                is NestedAttack.RecoverKeyResult.Failed -> {
                    onProgress?.invoke("Standard nested attack failed, trying hardnested...")
                }
            }
        } else {
            onProgress?.invoke("Card has true RNG (detected earlier) — using hardnested attack")
        }

        // Fall back to hardnested attack
        val hardnested = HardnestedAttack(rawClassic, tech.uidAsUInt)
        val recoveredKey =
            hardnested.recoverKey(
                knownKeyType = knownKeyType,
                knownSectorBlock = knownBlock,
                knownKey = knownKey,
                targetKeyType = 0x60,
                targetBlock = targetBlock,
                onProgress = onProgress,
            )

        // Reset PN533 to normal mode and reselect card after the attack.
        // The hardnested attack leaves the PN533 with parity/CRC disabled
        // and the card may be in crypto mode from the last verify auth.
        rawClassic.hardReselectCard()

        if (recoveredKey != null) {
            return verifyRecoveredKey(tech, sectorIndex, recoveredKey)
        }

        return null
    }

    /**
     * Verify a recovered key works for authentication and determine if it's Key A or Key B.
     */
    private suspend fun verifyRecoveredKey(
        tech: PN533ClassicTechnology,
        sectorIndex: Int,
        key: Long,
    ): Pair<ByteArray, Boolean>? {
        val keyBytes = longToKeyBytes(key)
        // Try as Key A first
        val authA = tech.authenticateSectorWithKeyA(sectorIndex, keyBytes)
        if (authA) return Pair(keyBytes, true)
        // Try as Key B
        val authB = tech.authenticateSectorWithKeyB(sectorIndex, keyBytes)
        if (authB) return Pair(keyBytes, false)
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
