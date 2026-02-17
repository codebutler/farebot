/*
 * Crypto1RecoveryTest.kt
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

package com.codebutler.farebot.card.classic.crypto1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the Crypto1 key recovery algorithm.
 *
 * Tests simulate the mfkey32 attack: given authentication data (uid, nonce,
 * reader nonce, reader response), recover the keystream, feed it to
 * lfsrRecovery32, and verify the correct key can be extracted by rolling
 * back the LFSR state.
 *
 * IMPORTANT: In the real MIFARE Classic protocol, the reader nonce (nR) phase
 * uses encrypted mode (isEncrypted=true). The forward simulation MUST use
 * encrypted mode for nR to produce the correct cipher state, otherwise the
 * keystream at the aR phase will be wrong and recovery will fail.
 */
class Crypto1RecoveryTest {

    /**
     * Simulate a full MIFARE Classic authentication and verify that
     * lfsrRecovery32 can recover the key from the observed data.
     *
     * This follows the mfkey32 attack approach:
     * 1. Initialize cipher with key, feed uid^nT (not encrypted)
     * 2. Process reader nonce nR (encrypted mode - as in real protocol)
     * 3. Generate keystream for reader response aR (generates ks2 with input=0)
     * 4. Recover LFSR state from ks2
     * 5. Roll back through ks2, nR (encrypted), and uid^nT to extract the key
     */
    @Test
    fun testRecoverKeyMfkey32Style() {
        val key = 0xA0A1A2A3A4A5L
        val uid = 0xDEADBEEFu
        val nT = 0x12345678u
        val nR = 0x87654321u

        // Simulate full auth with correct encrypted mode for nR
        val state = Crypto1State()
        state.loadKey(key)
        state.lfsrWord(uid xor nT, false) // init - not encrypted
        state.lfsrWord(nR, true) // reader nonce - ENCRYPTED (as in real protocol)
        val ks2 = state.lfsrWord(0u, false) // keystream for reader response (input=0)

        // Recovery: ks2 was generated with input=0
        val candidates = Crypto1Recovery.lfsrRecovery32(ks2, 0u)

        assertTrue(
            candidates.isNotEmpty(),
            "Should find at least one candidate state. ks2=0x${ks2.toString(16)}",
        )

        // Roll back each candidate to extract the key
        val foundKey = candidates.any { candidate ->
            val s = candidate.copy()
            s.lfsrRollbackWord(0u, false) // undo ks2 generation (input=0)
            s.lfsrRollbackWord(nR, true) // undo reader nonce (encrypted)
            s.lfsrRollbackWord(uid xor nT, false) // undo init
            s.getKey() == key
        }

        assertTrue(foundKey, "Correct key 0x${key.toString(16)} should be recoverable from candidates")
    }

    @Test
    fun testRecoverKeyMfkey32StyleDifferentKey() {
        val key = 0xFFFFFFFFFFFFL
        val uid = 0x01020304u
        val nT = 0xAABBCCDDu
        val nR = 0x11223344u

        val state = Crypto1State()
        state.loadKey(key)
        state.lfsrWord(uid xor nT, false)
        state.lfsrWord(nR, true) // ENCRYPTED
        val ks2 = state.lfsrWord(0u, false)

        val candidates = Crypto1Recovery.lfsrRecovery32(ks2, 0u)

        assertTrue(
            candidates.isNotEmpty(),
            "Should find at least one candidate. ks2=0x${ks2.toString(16)}",
        )

        val foundKey = candidates.any { candidate ->
            val s = candidate.copy()
            s.lfsrRollbackWord(0u, false)
            s.lfsrRollbackWord(nR, true)
            s.lfsrRollbackWord(uid xor nT, false)
            s.getKey() == key
        }

        assertTrue(foundKey, "Key FFFFFFFFFFFF should be recoverable")
    }

    @Test
    fun testRecoverKeyMfkey32StyleZeroKey() {
        val key = 0x000000000000L
        val uid = 0x11223344u
        val nT = 0x55667788u
        val nR = 0xAABBCCDDu

        val state = Crypto1State()
        state.loadKey(key)
        state.lfsrWord(uid xor nT, false)
        state.lfsrWord(nR, true) // ENCRYPTED
        val ks2 = state.lfsrWord(0u, false)

        val candidates = Crypto1Recovery.lfsrRecovery32(ks2, 0u)

        assertTrue(
            candidates.isNotEmpty(),
            "Should find at least one candidate. ks2=0x${ks2.toString(16)}",
        )

        val foundKey = candidates.any { candidate ->
            val s = candidate.copy()
            s.lfsrRollbackWord(0u, false)
            s.lfsrRollbackWord(nR, true)
            s.lfsrRollbackWord(uid xor nT, false)
            s.getKey() == key
        }

        assertTrue(foundKey, "Zero key should be recoverable")
    }

    @Test
    fun testRecoverKeyNestedStyle() {
        // Simulate nested authentication recovery.
        // The keystream is generated during cipher initialization (uid^nT feeding),
        // so the input parameter is uid^nT.
        val key = 0xA0A1A2A3A4A5L
        val uid = 0xDEADBEEFu
        val nT = 0x12345678u

        // Generate keystream during init (this is what encrypts the nested nonce)
        val state = Crypto1State()
        state.loadKey(key)
        val ks0 = state.lfsrWord(uid xor nT, false) // keystream while feeding uid^nT

        // Recovery: ks0 was generated with input=uid^nT
        val candidates = Crypto1Recovery.lfsrRecovery32(ks0, uid xor nT)

        assertTrue(
            candidates.isNotEmpty(),
            "Should find at least one candidate for nested recovery. ks0=0x${ks0.toString(16)}",
        )

        // Per mfkey32_nested: rollback uid^nT, then get key.
        val foundKey = candidates.any { candidate ->
            val s = candidate.copy()
            s.lfsrRollbackWord(uid xor nT, false)
            s.getKey() == key
        }

        // Also try direct extraction (in case the state is already at key position)
        val foundKeyDirect = candidates.any { candidate ->
            candidate.copy().getKey() == key
        }

        assertTrue(
            foundKey || foundKeyDirect,
            "Key should be recoverable from nested candidates",
        )
    }

    @Test
    fun testRecoverKeySimple() {
        // Simplest case: key -> ks (no init, no nR)
        // This tests the basic recovery without any protocol overhead.
        val key = 0xA0A1A2A3A4A5L

        val state = Crypto1State()
        state.loadKey(key)
        val ks = state.lfsrWord(0u, false) // keystream with no input

        val candidates = Crypto1Recovery.lfsrRecovery32(ks, 0u)

        assertTrue(
            candidates.isNotEmpty(),
            "Should find at least one candidate",
        )

        // Single rollback to undo the ks generation
        val foundKey = candidates.any { candidate ->
            val s = candidate.copy()
            s.lfsrRollbackWord(0u, false) // undo ks
            s.getKey() == key
        }

        assertTrue(foundKey, "Key should be recoverable from simple ks-only case")
    }

    @Test
    fun testRecoverKeyWithInit() {
        // Key -> init(uid^nT) -> ks
        val key = 0xA0A1A2A3A4A5L
        val uid = 0xDEADBEEFu
        val nT = 0x12345678u

        val state = Crypto1State()
        state.loadKey(key)
        state.lfsrWord(uid xor nT, false) // init
        val ks = state.lfsrWord(0u, false) // ks with input=0

        val candidates = Crypto1Recovery.lfsrRecovery32(ks, 0u)

        assertTrue(
            candidates.isNotEmpty(),
            "Should find at least one candidate",
        )

        val foundKey = candidates.any { candidate ->
            val s = candidate.copy()
            s.lfsrRollbackWord(0u, false) // undo ks
            s.lfsrRollbackWord(uid xor nT, false) // undo init
            s.getKey() == key
        }

        assertTrue(foundKey, "Key should be recoverable with init rollback")
    }

    @Test
    fun testNonceDistance() {
        val n1 = 0x01020304u
        val n2 = Crypto1.prngSuccessor(n1, 100u)
        val distance = Crypto1Recovery.nonceDistance(n1, n2)
        assertEquals(100u, distance, "Distance should be exactly 100 PRNG steps")
    }

    @Test
    fun testNonceDistanceZero() {
        val n = 0xDEADBEEFu
        val distance = Crypto1Recovery.nonceDistance(n, n)
        assertEquals(0u, distance, "Distance from nonce to itself should be 0")
    }

    @Test
    fun testNonceDistanceWraparound() {
        val n1 = 0xCAFEBABEu
        val steps = 50000u
        val n2 = Crypto1.prngSuccessor(n1, steps)
        val distance = Crypto1Recovery.nonceDistance(n1, n2)
        assertEquals(steps, distance, "Distance should work for large step counts within PRNG cycle")
    }

    @Test
    fun testNonceDistanceNotFound() {
        val distance = Crypto1Recovery.nonceDistance(0u, 0x12345678u)
        assertEquals(
            UInt.MAX_VALUE,
            distance,
            "Should return UInt.MAX_VALUE for unreachable nonces",
        )
    }

    @Test
    fun testFilterConstraintPruning() {
        // Verify that the number of candidates is reasonable (much less than 2^24).
        val key = 0x123456789ABCL
        val uid = 0x11223344u
        val nT = 0x55667788u
        val nR = 0xAABBCCDDu

        val state = Crypto1State()
        state.loadKey(key)
        state.lfsrWord(uid xor nT, false)
        state.lfsrWord(nR, true) // ENCRYPTED
        val ks2 = state.lfsrWord(0u, false)

        val candidates = Crypto1Recovery.lfsrRecovery32(ks2, 0u)

        assertTrue(
            candidates.size < 100000,
            "Filter constraints should produce a manageable number of candidates, got ${candidates.size}",
        )
    }
}
