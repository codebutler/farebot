/*
 * MetalBruteForceTest.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * Correctness and benchmark tests for MetalBruteForceEngine.
 * All tests gracefully skip on non-macOS platforms or when Metal
 * hardware is unavailable.
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

import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class MetalBruteForceTest {
    /**
     * Set up a known key, compute correct post-byte-0 (odd, even) state,
     * embed it among random states, and verify that Metal finds the same
     * candidates as the scalar engine.
     *
     * Uses acceptAll verifyFn so both engines return raw parity survivors.
     * This tests that the GPU parity check and key extraction match scalar.
     */
    @Test
    fun testMetalBruteForceMatchesScalar() =
        runBlocking {
            val metal =
                MetalBruteForceEngine.create() ?: run {
                    println("Metal unavailable, skipping test")
                    return@runBlocking
                }

            try {
                val key = 0x0A0B0C0D0E0FL
                val uid = 0xB7164F30u
                val nonce = 0x12345678u

                // Initialize cipher and compute encrypted nonce + parity
                val authState = Crypto1State()
                authState.loadKey(key)
                val ksWord = authState.lfsrWord(uid xor nonce, false)
                val encNonce = nonce xor ksWord

                val uidXorEnc = uid xor encNonce
                val inputBytes =
                    IntArray(4) { byteIdx ->
                        ((uidXorEnc shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
                    }
                val encBytes =
                    IntArray(4) { byteIdx ->
                        ((encNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
                    }

                // Compute expected parity bits
                val encState = Crypto1State()
                encState.loadKey(key)
                encState.lfsrWord(uid xor nonce, false)
                val encParBits = IntArray(4)
                for (byteIdx in 0 until 4) {
                    val ksByte = encState.lfsrByte(inputBytes[byteIdx], true)
                    val ksPar = Crypto1.filter(encState.odd)
                    val plainByte = encBytes[byteIdx] xor ksByte
                    encParBits[byteIdx] = Crypto1Auth.oddParity(plainByte) xor ksPar
                }

                // Compute correct post-byte-0 state
                val postByte0State = Crypto1State()
                postByte0State.loadKey(key)
                postByte0State.lfsrWord(uid xor nonce, false)
                postByte0State.lfsrByte(inputBytes[0], true)
                val correctOdd = (postByte0State.odd and 0xFFFFFFu).toInt()
                val correctEven = (postByte0State.even and 0xFFFFFFu).toInt()

                val rollbackInput = inputBytes[0]

                // Build state arrays: 100 odd × 200 even, with correct state embedded
                val rng = Random(42)
                val oddStates = IntArray(100) { rng.nextInt(1 shl 24) }
                val evenStates = IntArray(200) { rng.nextInt(1 shl 24) }
                oddStates[37] = correctOdd
                evenStates[142] = correctEven

                // Accept all parity survivors to compare raw engine output
                val acceptAll: (Long) -> Boolean = { true }

                // Run Metal
                val metalResults =
                    metal.bruteForce(
                        oddStates,
                        evenStates,
                        rollbackInput,
                        inputBytes,
                        encBytes,
                        encParBits,
                        acceptAll,
                        null,
                    )

                // Run Scalar
                val scalarResults =
                    ScalarBruteForceEngine().bruteForce(
                        oddStates,
                        evenStates,
                        rollbackInput,
                        inputBytes,
                        encBytes,
                        encParBits,
                        acceptAll,
                        null,
                    )

                println("Metal found ${metalResults.size} candidates, Scalar found ${scalarResults.size} candidates")

                val metalSet = metalResults.toSet()
                val scalarSet = scalarResults.toSet()

                // Both should find at least one candidate (the correct state passes parity)
                assertTrue(scalarResults.isNotEmpty(), "Scalar should find at least one parity survivor")
                assertTrue(metalResults.isNotEmpty(), "Metal should find at least one parity survivor")

                // Metal must produce identical results to scalar
                assertEquals(
                    scalarSet,
                    metalSet,
                    "Metal and Scalar candidate sets differ: " +
                        "metal_only=${metalSet - scalarSet}, scalar_only=${scalarSet - metalSet}",
                )
            } finally {
                metal.destroy()
            }
        }

    /**
     * Larger test: 1K odd × 5K even = 5M pairs.
     * Verifies Metal produces no false negatives compared to scalar.
     */
    @Test
    fun testMetalNoFalseNegatives() =
        runBlocking {
            val metal =
                MetalBruteForceEngine.create() ?: run {
                    println("Metal unavailable, skipping test")
                    return@runBlocking
                }

            try {
                val key = 0xAABBCCDDEEFFL
                val uid = 0x01020304u
                val nonce = 0xDEADBEEFu

                val authState = Crypto1State()
                authState.loadKey(key)
                val ksWord = authState.lfsrWord(uid xor nonce, false)
                val encNonce = nonce xor ksWord

                val uidXorEnc = uid xor encNonce
                val inputBytes =
                    IntArray(4) { byteIdx ->
                        ((uidXorEnc shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
                    }
                val encBytes =
                    IntArray(4) { byteIdx ->
                        ((encNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
                    }

                val encState = Crypto1State()
                encState.loadKey(key)
                encState.lfsrWord(uid xor nonce, false)
                val encParBits = IntArray(4)
                for (byteIdx in 0 until 4) {
                    val ksByte = encState.lfsrByte(inputBytes[byteIdx], true)
                    val ksPar = Crypto1.filter(encState.odd)
                    val plainByte = encBytes[byteIdx] xor ksByte
                    encParBits[byteIdx] = Crypto1Auth.oddParity(plainByte) xor ksPar
                }

                val postByte0State = Crypto1State()
                postByte0State.loadKey(key)
                postByte0State.lfsrWord(uid xor nonce, false)
                postByte0State.lfsrByte(inputBytes[0], true)
                val correctOdd = (postByte0State.odd and 0xFFFFFFu).toInt()
                val correctEven = (postByte0State.even and 0xFFFFFFu).toInt()

                val rollbackInput = inputBytes[0]

                // 1K odd × 5K even = 5M pairs
                val rng = Random(99)
                val oddStates = IntArray(1000) { rng.nextInt(1 shl 24) }
                val evenStates = IntArray(5000) { rng.nextInt(1 shl 24) }
                oddStates[500] = correctOdd
                evenStates[2500] = correctEven

                val acceptAll: (Long) -> Boolean = { true }

                val metalResults =
                    metal.bruteForce(
                        oddStates,
                        evenStates,
                        rollbackInput,
                        inputBytes,
                        encBytes,
                        encParBits,
                        acceptAll,
                        null,
                    )
                val scalarResults =
                    ScalarBruteForceEngine().bruteForce(
                        oddStates,
                        evenStates,
                        rollbackInput,
                        inputBytes,
                        encBytes,
                        encParBits,
                        acceptAll,
                        null,
                    )

                val metalSet = metalResults.toSet()
                val scalarSet = scalarResults.toSet()

                println("Metal: ${metalResults.size} candidates, Scalar: ${scalarResults.size} candidates")

                // Metal must find everything scalar finds (no false negatives)
                val missed = scalarSet - metalSet
                assertTrue(
                    missed.isEmpty(),
                    "Metal missed ${missed.size} keys that scalar found: ${missed.take(10)}",
                )

                // Metal should not produce extra keys (no false positives)
                val extra = metalSet - scalarSet
                assertTrue(
                    extra.isEmpty(),
                    "Metal produced ${extra.size} extra keys that scalar didn't find: ${extra.take(10)}",
                )
            } finally {
                metal.destroy()
            }
        }

    /**
     * Benchmark: 500 odd × 50K even = 25M pairs.
     * Prints throughput for Metal, SIMD, and Scalar engines.
     */
    @Test
    fun benchmarkMetalVsSimdVsScalar() =
        runBlocking {
            val metal =
                MetalBruteForceEngine.create() ?: run {
                    println("Metal unavailable, skipping benchmark")
                    return@runBlocking
                }

            try {
                println("Metal device: ${metal.deviceName}")

                val oddStates = IntArray(500) { i -> (i * 262139 + 0xABC) and 0xFFFFFF }
                val evenStates = IntArray(50_000) { i -> (i * 131071 + 0xDEF) and 0xFFFFFF }
                val rollbackInput = 0x42
                val inputBytes = intArrayOf(0x11, 0x22, 0x33, 0x44)
                val encBytes = intArrayOf(0xAA, 0xBB, 0xCC, 0xDD)
                val encParBits = intArrayOf(1, 0, 1, 0)
                val verifyFn: (Long) -> Boolean = { true }

                val totalPairs = oddStates.size.toLong() * evenStates.size

                // Warmup (smaller arrays)
                val warmOdd = oddStates.sliceArray(0 until 10)
                val warmEven = evenStates.sliceArray(0 until 1000)
                repeat(3) {
                    metal.bruteForce(
                        warmOdd,
                        warmEven,
                        rollbackInput,
                        inputBytes,
                        encBytes,
                        encParBits,
                        verifyFn,
                        null,
                    )
                }

                val simd = SimdBruteForceEngine()
                val scalar = ScalarBruteForceEngine()
                repeat(3) {
                    simd.bruteForce(warmOdd, warmEven, rollbackInput, inputBytes, encBytes, encParBits, verifyFn, null)
                    scalar.bruteForce(
                        warmOdd,
                        warmEven,
                        rollbackInput,
                        inputBytes,
                        encBytes,
                        encParBits,
                        verifyFn,
                        null,
                    )
                }

                // Benchmark Metal
                val mark1 = TimeSource.Monotonic.markNow()
                metal.bruteForce(oddStates, evenStates, rollbackInput, inputBytes, encBytes, encParBits, verifyFn, null)
                val metalElapsed = mark1.elapsedNow()

                // Benchmark SIMD
                val mark2 = TimeSource.Monotonic.markNow()
                simd.bruteForce(oddStates, evenStates, rollbackInput, inputBytes, encBytes, encParBits, verifyFn, null)
                val simdElapsed = mark2.elapsedNow()

                // Benchmark Scalar
                val mark3 = TimeSource.Monotonic.markNow()
                scalar.bruteForce(
                    oddStates,
                    evenStates,
                    rollbackInput,
                    inputBytes,
                    encBytes,
                    encParBits,
                    verifyFn,
                    null,
                )
                val scalarElapsed = mark3.elapsedNow()

                val metalMs = metalElapsed.inWholeMilliseconds.coerceAtLeast(1)
                val simdMs = simdElapsed.inWholeMilliseconds.coerceAtLeast(1)
                val scalarMs = scalarElapsed.inWholeMilliseconds.coerceAtLeast(1)
                val metalRate = totalPairs * 1000 / metalMs
                val simdRate = totalPairs * 1000 / simdMs
                val scalarRate = totalPairs * 1000 / scalarMs

                println("Total pairs: $totalPairs (${totalPairs / 1_000_000}M)")
                println("Metal:  $metalElapsed, rate=${metalRate / 1_000_000}M/s")
                println("SIMD:   $simdElapsed, rate=${simdRate / 1_000_000}M/s")
                println("Scalar: $scalarElapsed, rate=${scalarRate / 1_000_000}M/s")
                println("Metal vs SIMD:   ${"%.1f".format(metalRate.toDouble() / simdRate)}x")
                println("Metal vs Scalar: ${"%.1f".format(metalRate.toDouble() / scalarRate)}x")
            } finally {
                metal.destroy()
            }
        }

    /**
     * Verify that MetalBruteForceEngine.verifyKeys() produces the same
     * results as the CPU default (ScalarBruteForceEngine.verifyKeys()).
     *
     * Generates synthetic nonces for a known key by running the full
     * Crypto1 LFSR, then checks that:
     * - Both engines agree on which keys pass
     * - The correct key passes verification
     * - Wrong keys are rejected
     */
    @Test
    fun testMetalVerifyKeysMatchesCpu() {
        val metal =
            MetalBruteForceEngine.create() ?: run {
                println("Metal unavailable, skipping test")
                return
            }

        try {
            val key = 0x0A0B0C0D0E0FL
            val uid = 0xB7164F30u

            // Generate 8 synthetic nonces from the known key
            val rng = Random(123)
            val nonces = mutableListOf<VerifyNonceData>()

            for (n in 0 until 8) {
                val nonce = rng.nextInt().toUInt()

                // Compute encrypted nonce: load key, clock uid^nonce, XOR nonce
                val encState = Crypto1State()
                encState.loadKey(key)
                val ksWord = encState.lfsrWord(uid xor nonce, false)
                val encNonce = nonce xor ksWord

                // Compute encrypted parity using the same method as verifyKeyWithNonceCpu:
                // load key, then clock through uid^encNonce with isEncrypted=true,
                // checking parity at each 8-bit byte boundary.
                val parState = Crypto1State()
                parState.loadKey(key)
                val uidXorEnc = uid xor encNonce

                var encParity = 0
                for (byteIdx in 0 until 4) {
                    var ksByteVal = 0
                    for (bitIdx in 0 until 8) {
                        val i = byteIdx * 8 + bitIdx
                        val inputBit = Crypto1.bebit(uidXorEnc, i).toInt()
                        val ksBit = parState.lfsrBit(inputBit, true)
                        ksByteVal = ksByteVal or (ksBit shl bitIdx)
                    }
                    val ksPar = Crypto1.filter(parState.odd)
                    val encByte = ((encNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
                    val plainByte = encByte xor ksByteVal
                    val parBit = Crypto1Auth.oddParity(plainByte) xor ksPar
                    encParity = encParity or (parBit shl (3 - byteIdx))
                }

                nonces.add(VerifyNonceData(encNonce, encParity))
            }

            // Candidate keys: the correct key + several wrong keys
            val candidateKeys =
                listOf(
                    key, // correct
                    0xAABBCCDDEEFFL, // wrong
                    0x112233445566L, // wrong
                    0x000000000000L, // wrong
                    0xFFFFFFFFFFFFL, // wrong
                    0x0A0B0C0D0E00L, // off by one byte — wrong
                )

            // Run Metal GPU verification
            val metalResults = metal.verifyKeys(candidateKeys, nonces, uid)

            // Run CPU verification
            val cpuResults = ScalarBruteForceEngine().verifyKeys(candidateKeys, nonces, uid)

            println("Metal verifyKeys returned: $metalResults")
            println("CPU   verifyKeys returned: $cpuResults")

            // Both engines must return the same set of keys
            assertEquals(
                cpuResults.toSet(),
                metalResults.toSet(),
                "Metal and CPU verifyKeys results differ: " +
                    "metal=$metalResults, cpu=$cpuResults",
            )

            // The correct key must pass
            assertTrue(
                key in metalResults,
                "Correct key 0x${key.toString(16)} not found in Metal results: $metalResults",
            )
            assertTrue(
                key in cpuResults,
                "Correct key 0x${key.toString(16)} not found in CPU results: $cpuResults",
            )

            // Wrong keys must not pass
            val wrongKeys = candidateKeys.filter { it != key }
            for (wrongKey in wrongKeys) {
                assertTrue(
                    wrongKey !in metalResults,
                    "Wrong key 0x${wrongKey.toString(16)} should not be in Metal results",
                )
                assertTrue(
                    wrongKey !in cpuResults,
                    "Wrong key 0x${wrongKey.toString(16)} should not be in CPU results",
                )
            }

            println(
                "testMetalVerifyKeysMatchesCpu PASSED: " +
                    "${nonces.size} nonces, ${candidateKeys.size} candidates, " +
                    "${metalResults.size} passed (Metal), ${cpuResults.size} passed (CPU)",
            )
        } finally {
            metal.destroy()
        }
    }
}
