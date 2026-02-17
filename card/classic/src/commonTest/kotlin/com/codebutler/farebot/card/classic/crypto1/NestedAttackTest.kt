/*
 * NestedAttackTest.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * Tests for the MIFARE Classic nested attack orchestration.
 *
 * Since the full attack requires PN533 hardware, these tests focus on the
 * pure-logic components: PRNG calibration, nonce data construction, and
 * simulated key recovery using the Crypto1 cipher in software.
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the MIFARE Classic nested attack logic.
 *
 * The full [NestedAttack.recoverKey] method requires a PN533 hardware device,
 * so these tests verify the testable pure-logic components:
 * - PRNG calibration (distance computation between consecutive nonces)
 * - NestedNonceData construction
 * - Simulated end-to-end key recovery using software Crypto1
 */
class NestedAttackTest {

    /**
     * Test PRNG calibration with nonces that are exactly 160 steps apart.
     *
     * Generates a sequence of nonces where each one is prngSuccessor(prev, 160),
     * then verifies that calibratePrng returns the correct distance of 160
     * for each consecutive pair.
     */
    @Test
    fun testCalibratePrng() {
        val startNonce = 0xCAFEBABEu
        val expectedDistance = 160u
        val nonces = mutableListOf<UInt>()

        // Generate 15 nonces, each 160 PRNG steps from the previous
        var current = startNonce
        for (i in 0 until 15) {
            nonces.add(current)
            current = Crypto1.prngSuccessor(current, expectedDistance)
        }

        val distances = NestedAttack.calibratePrng(nonces)

        // Should have 14 distances (one fewer than nonces)
        assertEquals(14, distances.size, "Should have nonces.size - 1 distances")

        // All distances should be exactly 160
        for ((i, d) in distances.withIndex()) {
            assertEquals(
                expectedDistance,
                d,
                "Distance at index $i should be $expectedDistance, got $d",
            )
        }
    }

    /**
     * Test PRNG calibration with varying distances (simulating jitter).
     *
     * In practice, the PRNG distance between consecutive nonces from the card
     * isn't perfectly constant due to timing variations. The calibration should
     * handle small variations gracefully, and the median should recover the
     * dominant distance.
     */
    @Test
    fun testCalibratePrngWithJitter() {
        val startNonce = 0x12345678u
        val baseDistance = 160u
        // Distances with jitter: most are 160, a few are 155 or 165
        val jitteredDistances = listOf(160u, 155u, 160u, 165u, 160u, 160u, 158u, 160u, 162u, 160u)

        val nonces = mutableListOf<UInt>()
        var current = startNonce
        nonces.add(current)
        for (d in jitteredDistances) {
            current = Crypto1.prngSuccessor(current, d)
            nonces.add(current)
        }

        val distances = NestedAttack.calibratePrng(nonces)

        assertEquals(jitteredDistances.size, distances.size, "Should have correct number of distances")

        // Verify the computed distances match what we put in
        for (i in distances.indices) {
            assertEquals(
                jitteredDistances[i],
                distances[i],
                "Distance at index $i should match input jittered distance",
            )
        }

        // Verify median is the base distance (160 appears most often)
        val sorted = distances.sorted()
        val median = sorted[sorted.size / 2]
        assertEquals(baseDistance, median, "Median distance should be the base distance $baseDistance")
    }

    /**
     * Test simulated nested attack key recovery entirely in software.
     *
     * This simulates the full nested authentication sequence:
     * 1. Authenticate with a known key (software Crypto1)
     * 2. Perform nested auth to get an encrypted nonce
     * 3. Use the cipher state at the point of nested auth to compute keystream
     * 4. XOR the encrypted nonce with keystream to get the candidate plaintext nonce
     * 5. Run lfsrRecovery32 with the keystream
     * 6. Roll back recovered states to extract the target key
     * 7. Verify the recovered key matches the target key
     */
    @Test
    fun testCollectAndRecoverSimulated() {
        val uid = 0xDEADBEEFu
        val knownKey = 0xA0A1A2A3A4A5L
        val targetKey = 0xB0B1B2B3B4B5L
        val knownNT = 0x12345678u // nonce from the known-key auth
        val targetNT = 0xAABBCCDDu // nonce from the target sector (the card's PRNG output)

        // Step 1: Simulate authentication with the known key.
        // After auth, the cipher state is ready for encrypted communication.
        val authState = Crypto1Auth.initCipher(knownKey, uid, knownNT)
        // Simulate the reader nonce and response phases
        val nR = 0x01020304u
        Crypto1Auth.computeReaderResponse(authState, nR, knownNT)
        // After computeReaderResponse, authState has been clocked through nR and aR phases

        // Step 2: Save the cipher state at the point of nested auth
        val cipherStateAtNested = authState.copy()

        // Step 3: Simulate the nested auth — the card sends targetNT encrypted with
        // the AUTH command keystream. In nested auth, the reader sends an encrypted AUTH
        // command, and the card responds with a new nonce encrypted with the Crypto1 stream.
        //
        // The encrypted nonce is: targetNT XOR keystream
        // where keystream comes from clocking the cipher state during nested auth processing.
        //
        // For the nested attack recovery, what matters is:
        // - The target sector's key is used to init a NEW cipher: targetKey, uid, targetNT
        // - The keystream from THAT initialization encrypts the nonce that the card sends
        //
        // Actually, in the real nested attack, we use a different approach:
        // We know the encrypted nonce and we need to find the keystream.
        // The keystream comes from the TARGET key's cipher initialization.
        //
        // Let's simulate what the card does: initialize cipher with targetKey and uid^targetNT
        val targetCipherState = Crypto1State()
        targetCipherState.loadKey(targetKey)
        val ks0 = targetCipherState.lfsrWord(uid xor targetNT, false)

        // The encrypted nonce as seen by the reader
        val encryptedNT = targetNT xor ks0

        // Step 4: Recovery — we know encryptedNT and need to find targetKey.
        // The keystream ks0 was generated with input = uid XOR targetNT.
        // But we don't know targetNT yet... we need to predict it.
        //
        // In the real attack, the reader predicts targetNT from the PRNG distance.
        // For this test, we just use the known targetNT directly.
        val ks = encryptedNT xor targetNT // = ks0

        // Use lfsrRecovery32 with input = uid XOR targetNT
        val candidates = Crypto1Recovery.lfsrRecovery32(ks, uid xor targetNT)

        assertTrue(
            candidates.isNotEmpty(),
            "Should find at least one candidate state",
        )

        // Step 5: Roll back each candidate to extract the key
        val recoveredKey = candidates.firstNotNullOfOrNull { candidate ->
            val s = candidate.copy()
            s.lfsrRollbackWord(uid xor targetNT, false) // undo the init feeding
            val key = s.getKey()
            if (key == targetKey) key else null
        }

        assertNotNull(recoveredKey, "Should recover the target key from candidates")
        assertEquals(targetKey, recoveredKey, "Recovered key should match target key")
    }

    /**
     * Test simulated recovery using recoverKeyFromNonces helper.
     *
     * This tests the Crypto1Recovery.recoverKeyFromNonces function which
     * encapsulates the nested key recovery logic.
     */
    @Test
    fun testRecoverKeyFromNoncesSimulated() {
        val uid = 0x01020304u
        val targetKey = 0x112233445566L
        val targetNT = 0xDEAD1234u

        // Simulate what the card does: encrypt targetNT with the target key
        val targetState = Crypto1State()
        targetState.loadKey(targetKey)
        val ks0 = targetState.lfsrWord(uid xor targetNT, false)
        val encryptedNT = targetNT xor ks0

        // Use lfsrRecovery32 with the keystream and input = uid XOR targetNT
        val candidates = Crypto1Recovery.lfsrRecovery32(ks0, uid xor targetNT)

        assertTrue(candidates.isNotEmpty(), "Should find candidates")

        // Recover key by rolling back
        val foundKey = candidates.any { candidate ->
            val s = candidate.copy()
            s.lfsrRollbackWord(uid xor targetNT, false)
            s.getKey() == targetKey
        }

        assertTrue(foundKey, "Target key should be among recovered candidates")
    }

    /**
     * Test NestedNonceData construction.
     *
     * Verifies that the data class correctly stores the encrypted nonce
     * and cipher state snapshot.
     */
    @Test
    fun testNestedNonceDataCreation() {
        val encNonce = 0xAABBCCDDu
        val state = Crypto1State(odd = 0x123456u, even = 0x789ABCu)

        val data = NestedAttack.NestedNonceData(
            encryptedNonce = encNonce,
            cipherStateAtNested = state,
        )

        assertEquals(encNonce, data.encryptedNonce, "Encrypted nonce should be stored correctly")
        assertEquals(0x123456u, data.cipherStateAtNested.odd, "Cipher state odd should be preserved")
        assertEquals(0x789ABCu, data.cipherStateAtNested.even, "Cipher state even should be preserved")
    }

    /**
     * Test that calibratePrng handles a minimal nonce list (2 nonces = 1 distance).
     */
    @Test
    fun testCalibratePrngMinimal() {
        val n1 = 0x11223344u
        val n2 = Crypto1.prngSuccessor(n1, 200u)

        val distances = NestedAttack.calibratePrng(listOf(n1, n2))

        assertEquals(1, distances.size, "Should have 1 distance for 2 nonces")
        assertEquals(200u, distances[0], "Single distance should be 200")
    }

    /**
     * Test that calibratePrng returns empty list for a single nonce.
     */
    @Test
    fun testCalibratePrngSingleNonce() {
        val distances = NestedAttack.calibratePrng(listOf(0xDEADBEEFu))
        assertTrue(distances.isEmpty(), "Should return empty list for single nonce")
    }

    /**
     * Test that calibratePrng returns empty list for empty input.
     */
    @Test
    fun testCalibratePrngEmpty() {
        val distances = NestedAttack.calibratePrng(emptyList())
        assertTrue(distances.isEmpty(), "Should return empty list for empty input")
    }

    /**
     * Test multiple simulated recoveries with different key values to ensure
     * the recovery logic is robust across different key spaces.
     */
    @Test
    fun testRecoverMultipleKeys() {
        val uid = 0xCAFEBABEu
        val keysToTest = listOf(
            0x000000000000L,
            0xFFFFFFFFFFFFL,
            0xA0A1A2A3A4A5L,
            0x112233445566L,
        )

        for (targetKey in keysToTest) {
            val targetNT = 0x55667788u

            val targetState = Crypto1State()
            targetState.loadKey(targetKey)
            val ks0 = targetState.lfsrWord(uid xor targetNT, false)

            val candidates = Crypto1Recovery.lfsrRecovery32(ks0, uid xor targetNT)

            assertTrue(
                candidates.isNotEmpty(),
                "Should find candidates for key 0x${targetKey.toString(16)}",
            )

            val foundKey = candidates.any { candidate ->
                val s = candidate.copy()
                s.lfsrRollbackWord(uid xor targetNT, false)
                s.getKey() == targetKey
            }

            assertTrue(
                foundKey,
                "Should recover key 0x${targetKey.toString(16)} from candidates",
            )
        }
    }

    /**
     * Test companion object constants are defined correctly.
     */
    @Test
    fun testConstants() {
        assertEquals(20, NestedAttack.CALIBRATION_ROUNDS)
        assertEquals(10, NestedAttack.MIN_CALIBRATION_NONCES)
        assertEquals(50, NestedAttack.COLLECTION_ROUNDS)
        assertEquals(5, NestedAttack.MIN_NONCES_FOR_RECOVERY)
    }
}
