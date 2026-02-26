/*
 * SimdBitsliceTest.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * Tests for the SIMD bitsliced Crypto1 operations. Each test verifies
 * that the bitsliced (SIMD) operation produces identical results to the
 * scalar Crypto1State implementation.
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

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class SimdBitsliceTest {
    // ---- Filter function tests ----

    /**
     * Pack BATCH_SIZE random states, compare SIMD filter vs scalar Crypto1.filter()
     * for every state in the batch.
     */
    @Test
    fun testSimdFilterMatchesScalar() {
        val batchSize = SimdBitslice.BATCH_SIZE
        val rng = Random(42)

        // Generate random 20-bit odd-half values
        val oddValues = IntArray(batchSize) { rng.nextInt(1 shl 20) }

        // Pack into bitsliced form (only odd half matters for filter)
        val evenValues = IntArray(batchSize) // don't care
        val bs = SimdBitslice.BitslicedState.pack(evenValues, oddValues)

        // Compute SIMD filter
        val simdResult = SimdBitslice.filter(bs.odd)

        // Extract and compare each state
        val lanes = LongArray(SimdBitslice.species.length())
        simdResult.intoArray(lanes, 0)

        for (i in 0 until batchSize) {
            val lane = i / 64
            val bit = i % 64
            val simdBit = ((lanes[lane] shr bit) and 1L).toInt()
            val scalarBit = Crypto1.filter(oddValues[i].toUInt())
            assertEquals(
                scalarBit,
                simdBit,
                "Filter mismatch at index $i (oddValue=0x${oddValues[i].toString(16)}): " +
                    "scalar=$scalarBit, simd=$simdBit",
            )
        }
    }

    /**
     * Test all 2^20 filter inputs in batches to verify exhaustive correctness.
     */
    @Test
    fun testSimdFilterExhaustive() {
        val batchSize = SimdBitslice.BATCH_SIZE
        val totalInputs = 1 shl 20 // 1,048,576

        var offset = 0
        while (offset < totalInputs) {
            val count = minOf(batchSize, totalInputs - offset)
            val oddValues = IntArray(count) { offset + it }
            val evenValues = IntArray(count) // don't care

            // Pad to BATCH_SIZE if needed
            val paddedOdd =
                if (count < batchSize) {
                    IntArray(batchSize).also { oddValues.copyInto(it) }
                } else {
                    oddValues
                }
            val paddedEven = IntArray(paddedOdd.size)

            val bs = SimdBitslice.BitslicedState.pack(paddedEven, paddedOdd)
            val simdResult = SimdBitslice.filter(bs.odd)

            val lanes = LongArray(SimdBitslice.species.length())
            simdResult.intoArray(lanes, 0)

            for (i in 0 until count) {
                val lane = i / 64
                val bit = i % 64
                val simdBit = ((lanes[lane] shr bit) and 1L).toInt()
                val scalarBit = Crypto1.filter((offset + i).toUInt())
                assertEquals(
                    scalarBit,
                    simdBit,
                    "Exhaustive filter mismatch at x=0x${(offset + i).toString(16)}: " +
                        "scalar=$scalarBit, simd=$simdBit",
                )
            }

            offset += count
        }
    }

    // ---- LFSR rollback tests ----

    /**
     * Compare SIMD rollback of a single bit against scalar.
     */
    @Test
    fun testSimdRollbackSingleBitMatchesScalar() {
        // Use just 2 states for easier debugging
        val evenValues = intArrayOf(0x123456, 0xABCDEF)
        val oddValues = intArrayOf(0x654321, 0xFEDCBA)
        val inputBitVal = 1

        // Pad to BATCH_SIZE
        val paddedEven =
            IntArray(SimdBitslice.BATCH_SIZE).also {
                evenValues.copyInto(it)
            }
        val paddedOdd =
            IntArray(SimdBitslice.BATCH_SIZE).also {
                oddValues.copyInto(it)
            }

        // Verify pack/unpack roundtrip first
        val bsCheck = SimdBitslice.BitslicedState.pack(paddedEven, paddedOdd)
        for (i in evenValues.indices) {
            val (uEven, uOdd) = bsCheck.unpackState(i)
            assertEquals(
                evenValues[i].toUInt(),
                uEven,
                "Pack/unpack even mismatch at index $i",
            )
            assertEquals(
                oddValues[i].toUInt(),
                uOdd,
                "Pack/unpack odd mismatch at index $i",
            )
        }

        // SIMD rollback 1 bit
        val bs = SimdBitslice.BitslicedState.pack(paddedEven, paddedOdd)
        val inputBit = if (inputBitVal != 0) SimdBitslice.ONES else SimdBitslice.ZEROS
        SimdBitslice.lfsrRollbackBit(bs.even, bs.odd, inputBit, true)

        for (i in evenValues.indices) {
            val scalarState = Crypto1State(oddValues[i].toUInt(), evenValues[i].toUInt())
            scalarState.lfsrRollbackBit(inputBitVal, true)

            val (simdEven, simdOdd) = bs.unpackState(i)
            assertEquals(
                scalarState.even and 0xFFFFFFu,
                simdEven and 0xFFFFFFu,
                "Rollback bit even mismatch at index $i: " +
                    "scalar=0x${(scalarState.even and 0xFFFFFFu).toString(16)}, " +
                    "simd=0x${(simdEven and 0xFFFFFFu).toString(16)}",
            )
            assertEquals(
                scalarState.odd and 0xFFFFFFu,
                simdOdd and 0xFFFFFFu,
                "Rollback bit odd mismatch at index $i: " +
                    "scalar=0x${(scalarState.odd and 0xFFFFFFu).toString(16)}, " +
                    "simd=0x${(simdOdd and 0xFFFFFFu).toString(16)}",
            )
        }
    }

    /**
     * Compare SIMD rollback byte against scalar Crypto1State.lfsrRollbackByte()
     * for a batch of random states.
     */
    @Test
    fun testSimdRollbackByteMatchesScalar() {
        val batchSize = SimdBitslice.BATCH_SIZE
        val rng = Random(123)

        val evenValues = IntArray(batchSize) { rng.nextInt(1 shl 24) }
        val oddValues = IntArray(batchSize) { rng.nextInt(1 shl 24) }
        val inputByte = 0xA5

        // SIMD rollback
        val bs = SimdBitslice.BitslicedState.pack(evenValues, oddValues)
        SimdBitslice.lfsrRollbackByte(bs.even, bs.odd, inputByte, true)

        // Compare each state against scalar rollback
        for (i in 0 until batchSize) {
            val scalarState = Crypto1State(oddValues[i].toUInt(), evenValues[i].toUInt())
            scalarState.lfsrRollbackByte(inputByte, true)

            val (simdEven, simdOdd) = bs.unpackState(i)
            assertEquals(
                scalarState.even and 0xFFFFFFu,
                simdEven and 0xFFFFFFu,
                "Rollback even mismatch at index $i",
            )
            assertEquals(
                scalarState.odd and 0xFFFFFFu,
                simdOdd and 0xFFFFFFu,
                "Rollback odd mismatch at index $i",
            )
        }
    }

    // ---- LFSR forward tests ----

    /**
     * Compare SIMD forward byte against scalar Crypto1State.lfsrByte()
     * for a batch of random states. Checks both resulting state and keystream output.
     */
    @Test
    fun testSimdForwardByteMatchesScalar() {
        val batchSize = SimdBitslice.BATCH_SIZE
        val rng = Random(456)

        val evenValues = IntArray(batchSize) { rng.nextInt(1 shl 24) }
        val oddValues = IntArray(batchSize) { rng.nextInt(1 shl 24) }
        val inputByte = 0x3C

        // SIMD forward byte
        val bs = SimdBitslice.BitslicedState.pack(evenValues, oddValues)
        val (ksBits, ksParity) = SimdBitslice.lfsrForwardByte(bs.even, bs.odd, inputByte, true)

        // Also get filter preview after forward byte
        val filterPreview = SimdBitslice.filterPreview(bs.odd)

        // Extract keystream bits and parity from SIMD result
        val ksLanes = Array(8) { LongArray(SimdBitslice.species.length()) }
        for (bit in 0 until 8) {
            ksBits[bit].intoArray(ksLanes[bit], 0)
        }
        val parLanes = LongArray(SimdBitslice.species.length())
        ksParity.intoArray(parLanes, 0)
        val filterLanes = LongArray(SimdBitslice.species.length())
        filterPreview.intoArray(filterLanes, 0)

        for (i in 0 until batchSize) {
            val lane = i / 64
            val bit = i % 64

            // Reconstruct SIMD keystream byte
            var simdKsByte = 0
            for (b in 0 until 8) {
                simdKsByte = simdKsByte or (((ksLanes[b][lane] shr bit) and 1L).toInt() shl b)
            }

            // SIMD parity
            val simdKsParity = ((parLanes[lane] shr bit) and 1L).toInt()

            // SIMD filter preview
            val simdFilterPreview = ((filterLanes[lane] shr bit) and 1L).toInt()

            // Scalar forward byte
            val scalarState = Crypto1State(oddValues[i].toUInt(), evenValues[i].toUInt())
            val scalarKsByte = scalarState.lfsrByte(inputByte, true)
            val scalarFilterPreview = Crypto1.filter(scalarState.odd)

            // Compute scalar parity (XOR of all 8 keystream bits)
            var scalarParity = 0
            for (b in 0 until 8) {
                scalarParity = scalarParity xor ((scalarKsByte shr b) and 1)
            }

            assertEquals(
                scalarKsByte,
                simdKsByte,
                "Forward byte keystream mismatch at index $i",
            )
            assertEquals(
                scalarParity,
                simdKsParity,
                "Forward byte parity mismatch at index $i",
            )
            assertEquals(
                scalarFilterPreview,
                simdFilterPreview,
                "Filter preview mismatch at index $i",
            )

            // Also verify resulting state matches
            val (simdEven, simdOdd) = bs.unpackState(i)
            assertEquals(
                scalarState.even and 0xFFFFFFu,
                simdEven and 0xFFFFFFu,
                "Forward even state mismatch at index $i",
            )
            assertEquals(
                scalarState.odd and 0xFFFFFFu,
                simdOdd and 0xFFFFFFu,
                "Forward odd state mismatch at index $i",
            )
        }
    }

    // ---- Parity check batch tests ----

    /**
     * Compare full parity check batch against the scalar inner loop.
     *
     * Sets up a known key, generates the encryption parameters, then verifies
     * that the SIMD batch correctly identifies which (odd, even) pairs pass
     * the 4-byte parity check.
     */
    @Test
    fun testSimdParityBatchMatchesScalar() {
        val batchSize = SimdBitslice.BATCH_SIZE
        val rng = Random(789)

        // Simulate the brute force scenario matching HardnestedAttack.bruteForceStateList().
        //
        // The attack receives:
        //   - encryptedNonce: the 4-byte encrypted nonce from the card
        //   - encryptedParity: 4 parity bits
        //   - uid: card UID
        //   - bestFirstByte: first byte of encryptedNonce (used for rollback)
        //
        // The brute force computes:
        //   - uidXorEnc = uid ^ encryptedNonce
        //   - inputBytes[i] = byte i of uidXorEnc
        //   - encBytes[i] = byte i of encryptedNonce
        //   - encParBits[i] = parity bit i
        //   - rollbackInput = (uid >> 24) ^ bestFirstByte = inputBytes[0]

        val key = 0x0A0B0C0D0E0FL
        val uid = 0xB7164F30u
        val nonce = 0x12345678u

        // Initialize cipher with standard auth init, then generate encrypted nonce
        // using lfsrWord (which matches the real protocol's 32-bit word processing).
        val authState = Crypto1State()
        authState.loadKey(key)
        val ksWord = authState.lfsrWord(uid xor nonce, false)
        // After lfsrWord, authState is at the authenticated state.
        // The encrypted nonce = nonce XOR ksWord (big-endian word)
        val encNonce = nonce xor ksWord

        // Now compute encrypted parity using lfsrByte (matching the brute force flow).
        // The brute force uses inputBytes = bytes of uid ^ encNonce.
        val uidXorEnc = uid xor encNonce
        val inputBytes =
            IntArray(4) { byteIdx ->
                ((uidXorEnc shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            }
        val encBytes =
            IntArray(4) { byteIdx ->
                ((encNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            }

        // Re-init cipher to compute expected parity at each byte boundary
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

        // The "correct" post-byte-0 state: advance the auth-init state by 1 byte
        val postByte0State = Crypto1State()
        postByte0State.loadKey(key)
        postByte0State.lfsrWord(uid xor nonce, false)
        postByte0State.lfsrByte(inputBytes[0], true)
        val correctOdd = (postByte0State.odd and 0xFFFFFFu).toInt()
        val correctEven = (postByte0State.even and 0xFFFFFFu).toInt()

        // rollbackInput undoes byte 0 processing
        // In the real attack: rollbackInput = (uid >> 24) ^ bestFirstByte
        // bestFirstByte = encNonce >> 24 & 0xFF = encBytes[0]
        // So rollbackInput = (uid >> 24 & 0xFF) ^ encBytes[0] = inputBytes[0]
        val rollbackInput = inputBytes[0]

        // First verify the correct state passes scalar parity check (sanity)
        val verifyState = Crypto1State()
        verifyState.odd = correctOdd.toUInt()
        verifyState.even = correctEven.toUInt()
        verifyState.lfsrRollbackByte(rollbackInput, true)
        for (byteIdx in 0 until 4) {
            val ksByte = verifyState.lfsrByte(inputBytes[byteIdx], true)
            val ksPar = Crypto1.filter(verifyState.odd)
            val plainByte = encBytes[byteIdx] xor ksByte
            val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
            assertEquals(
                encParBits[byteIdx],
                expectedEncPar,
                "Scalar sanity check failed at byte $byteIdx",
            )
        }

        // Create a batch of even states: most random, one correct
        val evenStates = IntArray(batchSize) { rng.nextInt(1 shl 24) }
        val correctIdx = rng.nextInt(batchSize)
        evenStates[correctIdx] = correctEven

        // Broadcast the correct odd state
        val broadcastOdd = SimdBitslice.broadcastOddState(correctOdd)
        val evenBatch = SimdBitslice.BitslicedState.packEven(evenStates)

        // SIMD parity check
        val survivors =
            SimdBitslice.checkParityBatchPreTransposed(
                broadcastOdd,
                evenBatch,
                rollbackInput,
                inputBytes,
                encBytes,
                encParBits,
            )

        // Extract survivor indices
        val survivorIndices = SimdBitslice.extractSurvivors(survivors)

        // The correct index should be a survivor
        assert(survivorIndices.contains(correctIdx)) {
            "Correct state at index $correctIdx not found in survivors: $survivorIndices"
        }

        // Verify each survivor with scalar code
        val scalarState = Crypto1State()
        for (idx in survivorIndices) {
            scalarState.odd = correctOdd.toUInt()
            scalarState.even = evenStates[idx].toUInt()
            scalarState.lfsrRollbackByte(rollbackInput, true)

            var parityOk = true
            for (byteIdx in 0 until 4) {
                val ksByte = scalarState.lfsrByte(inputBytes[byteIdx], true)
                val ksPar = Crypto1.filter(scalarState.odd)
                val plainByte = encBytes[byteIdx] xor ksByte
                val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
                if (expectedEncPar != encParBits[byteIdx]) {
                    parityOk = false
                    break
                }
            }
            assert(parityOk) {
                "SIMD survivor at index $idx failed scalar parity check"
            }
        }

        // Verify non-survivors fail scalar parity check (spot check a few)
        val nonSurvivorCount = batchSize - survivorIndices.size
        if (nonSurvivorCount > 0) {
            var checked = 0
            for (idx in 0 until batchSize) {
                if (survivorIndices.contains(idx)) continue
                if (checked >= 100) break // spot check 100

                scalarState.odd = correctOdd.toUInt()
                scalarState.even = evenStates[idx].toUInt()
                scalarState.lfsrRollbackByte(rollbackInput, true)

                var parityOk = true
                for (byteIdx in 0 until 4) {
                    val ksByte = scalarState.lfsrByte(inputBytes[byteIdx], true)
                    val ksPar = Crypto1.filter(scalarState.odd)
                    val plainByte = encBytes[byteIdx] xor ksByte
                    val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
                    if (expectedEncPar != encParBits[byteIdx]) {
                        parityOk = false
                        break
                    }
                }
                assert(!parityOk) {
                    "Non-survivor at index $idx passed scalar parity check but SIMD said fail"
                }
                checked++
            }
        }
    }
}
