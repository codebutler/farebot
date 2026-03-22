/*
 * BruteForceEngine.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * Abstraction for the hardnested brute force inner loop, allowing
 * platform-specific implementations (e.g., SIMD on JVM via Vector API).
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

package com.codebutler.farebot.keymanager.crypto1

/**
 * Nonce data for key verification: encrypted nonce + encrypted parity.
 */
data class VerifyNonceData(
    val encryptedNonce: UInt,
    val encryptedParity: Int,
)

/**
 * Interface for the hardnested brute force inner loop.
 *
 * Given arrays of candidate odd/even LFSR half-states (post-byte-0),
 * iterates all (odd, even) pairs, rolls back one byte, checks 4-byte
 * parity against the primary nonce, extracts candidate keys, and verifies
 * them against additional nonces.
 *
 * Implementations may use scalar Crypto1State operations (portable) or
 * SIMD bitsliced operations (JVM Vector API) for higher throughput.
 */
interface BruteForceEngine {
    /** Human-readable name of this engine (e.g., "Metal GPU: Apple M2 Max", "SIMD", "Scalar"). */
    val name: String

    /**
     * Brute force all (odd, even) state pairs to find valid keys.
     *
     * @param oddStates Candidate odd LFSR half-states (post-byte-0)
     * @param evenStates Candidate even LFSR half-states (post-byte-0)
     * @param rollbackInput Input byte for rolling back 1 byte: (uid>>24) ^ bestFirstByte
     * @param inputBytes 4-byte array of uid^encNonce bytes (big-endian, byte 0 first)
     * @param encBytes 4-byte array of encrypted nonce bytes (big-endian, byte 0 first)
     * @param encParBits 4-element array of encrypted parity bits (one per byte)
     * @param verifyFn Verification function: returns true if a candidate key passes
     *   multi-nonce verification. Called only for candidates that pass the primary
     *   nonce parity check.
     * @param onProgress Optional callback invoked after processing each odd state.
     *   Reports (tested pair count, running candidate count).
     * @return List of verified candidate keys (48-bit values as Long)
     */
    suspend fun bruteForce(
        oddStates: IntArray,
        evenStates: IntArray,
        rollbackInput: Int,
        inputBytes: IntArray,
        encBytes: IntArray,
        encParBits: IntArray,
        verifyFn: (Long) -> Boolean,
        onProgress: ((tested: Long, candidates: Int) -> Unit)?,
    ): List<Long>

    /**
     * Verify candidate keys against multiple nonces.
     *
     * Default implementation runs Crypto1 LFSR on CPU per key/nonce.
     * GPU-accelerated engines can override this to dispatch verification
     * to the GPU for much higher throughput (~35x speedup).
     *
     * @param keys Candidate 48-bit keys to verify.
     * @param nonces Nonce data to verify against (encrypted nonces + parity).
     * @param uid Card UID.
     * @return Keys that passed verification against all nonces.
     */
    fun verifyKeys(
        keys: List<Long>,
        nonces: List<VerifyNonceData>,
        uid: UInt,
    ): List<Long> {
        if (nonces.isEmpty()) return keys
        val state = Crypto1State()
        return keys.filter { key ->
            nonces.all { vn ->
                verifyKeyWithNonceCpu(key, vn.encryptedNonce, vn.encryptedParity, uid, state)
            }
        }
    }
}

/**
 * CPU-based nonce verification: loads key, clocks LFSR through uid^encNonce,
 * checks parity for all 4 bytes.
 *
 * Port of HardnestedAttack.verifyKeyWithNonce(), extracted here so it can
 * be shared by BruteForceEngine.verifyKeys() default implementation.
 */
internal fun verifyKeyWithNonceCpu(
    key: Long,
    encNonce: UInt,
    encParity: Int,
    uid: UInt,
    state: Crypto1State,
): Boolean {
    state.loadKey(key)
    val uidXorEnc = uid xor encNonce

    for (byteIdx in 0 until 4) {
        var ksByteVal = 0
        for (bitIdx in 0 until 8) {
            val i = byteIdx * 8 + bitIdx
            val inputBit = Crypto1.bebit(uidXorEnc, i).toInt()
            val ksBit = state.lfsrBit(inputBit, true)
            ksByteVal = ksByteVal or (ksBit shl bitIdx)
        }

        val ksPar = Crypto1.filter(state.odd)
        val encByte = ((encNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
        val plainByte = encByte xor ksByteVal
        val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
        val encParBit = (encParity shr (3 - byteIdx)) and 1

        if (expectedEncPar != encParBit) return false
    }
    return true
}

/**
 * Scalar (non-SIMD) brute force engine using Crypto1State operations.
 *
 * This is the portable fallback implementation that works on all platforms.
 * It iterates all (odd, even) pairs one at a time, using the standard
 * Crypto1State rollback, forward, and parity check operations.
 *
 * Extracted from HardnestedAttack.bruteForceStateList() inner loop.
 */
class ScalarBruteForceEngine : BruteForceEngine {
    override val name: String = "Scalar"

    override suspend fun bruteForce(
        oddStates: IntArray,
        evenStates: IntArray,
        rollbackInput: Int,
        inputBytes: IntArray,
        encBytes: IntArray,
        encParBits: IntArray,
        verifyFn: (Long) -> Boolean,
        onProgress: ((tested: Long, candidates: Int) -> Unit)?,
    ): List<Long> {
        var tested = 0L
        val candidates = mutableListOf<Long>()
        val state = Crypto1State()

        for (oi in oddStates.indices) {
            val oddState = oddStates[oi].toUInt()
            for (evenState in evenStates) {
                tested++

                // Candidates are post-byte-0 states (from sum property filtering).
                // Roll back 1 byte to recover the initial key state before
                // processing uid^encNonce. Port of Proxmark3's verify_key() which
                // calls lfsr_rollback_byte(&pcs, (cuid>>24)^best_first_bytes[0], true).
                state.odd = oddState
                state.even = evenState.toUInt()
                state.lfsrRollbackByte(rollbackInput, true)

                // Now state is at the initial key state. Process uid^encNonce
                // and check parity.
                var parityOk = true
                for (byteIdx in 0 until 4) {
                    val ksByte = state.lfsrByte(inputBytes[byteIdx], true)
                    val ksPar = Crypto1.filter(state.odd)
                    val plainByte = encBytes[byteIdx] xor ksByte
                    val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
                    if (expectedEncPar != encParBits[byteIdx]) {
                        parityOk = false
                        break
                    }
                }
                if (!parityOk) continue

                // Roll back again to extract the key from the initial state.
                state.odd = oddState
                state.even = evenState.toUInt()
                state.lfsrRollbackByte(rollbackInput, true)
                val candidateKey = state.getKey()

                // Multi-nonce verification
                if (!verifyFn(candidateKey)) continue
                candidates.add(candidateKey)
            }
            onProgress?.invoke(tested, candidates.size)
        }
        return candidates
    }
}

/**
 * Factory function to create the platform-appropriate [BruteForceEngine].
 *
 * On JVM, this will eventually return a SIMD-accelerated engine when available.
 * On all other platforms, returns [ScalarBruteForceEngine].
 */
expect fun createBruteForceEngine(): BruteForceEngine

/** Returns the number of available processors on the current platform. */
expect fun availableProcessors(): Int
