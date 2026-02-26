/*
 * SimdBruteForceEngine.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * SIMD-accelerated brute force engine for the hardnested attack using the
 * JDK Vector API (jdk.incubator.vector). Processes multiple Crypto1 states
 * in parallel using bitsliced operations.
 *
 * The bitsliced representation stores each bit position of N states as a
 * single SIMD lane, allowing N states to be processed simultaneously with
 * a single vector instruction. On ARM NEON (128-bit), N=128; on AVX2, N=256.
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

import jdk.incubator.vector.LongVector
import jdk.incubator.vector.VectorOperators
import jdk.incubator.vector.VectorSpecies

// ---- LongVector extension helpers ----
// JDK Vector API uses lanewise() for XOR and NOT operations.
// These extensions provide concise syntax matching the natural Boolean operators.

/** Bitwise XOR of two LongVectors. */
private fun LongVector.xor(other: LongVector): LongVector = this.lanewise(VectorOperators.XOR, other)

/** Bitwise NOT of a LongVector. */
private fun LongVector.inv(): LongVector = this.lanewise(VectorOperators.NOT)

/**
 * SIMD bitsliced Crypto1 operations using the JDK Vector API.
 *
 * All operations work on [LongVector] values where each bit position
 * represents the corresponding bit of a different Crypto1 state. This
 * allows SPECIES_PREFERRED.length() * 64 states to be processed in
 * parallel with each vector operation.
 */
object SimdBitslice {
    /** Vector species — auto-detects best SIMD width (128-bit NEON, 256-bit AVX2, etc.) */
    val species: VectorSpecies<Long> = LongVector.SPECIES_PREFERRED

    /** Number of states processed in parallel: species.length() * 64 */
    val BATCH_SIZE: Int = species.length() * 64

    /** All-ones vector (broadcast -1L) */
    val ONES: LongVector = LongVector.broadcast(species, -1L)

    /** All-zeros vector */
    val ZEROS: LongVector = LongVector.zero(species)

    // ---- LFSR polynomial tap positions ----
    // LF_POLY_ODD = 0x29CE5C → bits: 2,3,4,6,9,10,11,14,15,16,19,21
    private val LF_POLY_ODD_TAPS = intArrayOf(2, 3, 4, 6, 9, 10, 11, 14, 15, 16, 19, 21)

    // LF_POLY_EVEN = 0x870804 → bits: 2,11,16,17,18,23
    private val LF_POLY_EVEN_TAPS = intArrayOf(2, 11, 16, 17, 18, 23)

    // ---- Layer 1: 4-input Boolean functions ----

    /**
     * f20a: Truth table 0xf22c (used for nibbles 0, 2, 3).
     *
     * Derived from Proxmark3's crypto1_bs_f20b with reversed bit ordering.
     * Formula: ((c AND d) OR b) XOR ((c XOR d) AND (a OR b))
     *
     * 6 operations, no NOT gates needed.
     */
    private fun f20a(
        a: LongVector,
        b: LongVector,
        c: LongVector,
        d: LongVector,
    ): LongVector {
        val cd = c.and(d)
        val cxd = c.xor(d)
        val left = cd.or(b)
        val right = cxd.and(a.or(b))
        return left.xor(right)
    }

    /**
     * f20b: Truth table 0xd938 (used for nibbles 1, 4).
     *
     * Derived from Proxmark3's crypto1_bs_f20a with reversed bit ordering.
     * Formula: ((c OR d) XOR (a AND d)) XOR (b AND ((c XOR d) OR a))
     *
     * 6 operations, no NOT gates needed.
     */
    private fun f20b(
        a: LongVector,
        b: LongVector,
        c: LongVector,
        d: LongVector,
    ): LongVector {
        val cod = c.or(d)
        val ad = a.and(d)
        val cxd = c.xor(d)
        val left = cod.xor(ad)
        val right = b.and(cxd.or(a))
        return left.xor(right)
    }

    /**
     * f20c: 5-input combiner function. Truth table 0xEC57E80A.
     *
     * Formula: (a OR ((b OR e) AND (d XOR e))) XOR ((a XOR (b AND d)) AND ((c XOR d) OR (b AND e)))
     *
     * 8 operations total.
     */
    private fun f20c(
        a: LongVector,
        b: LongVector,
        c: LongVector,
        d: LongVector,
        e: LongVector,
    ): LongVector {
        val bd = b.and(d)
        val be = b.and(e)
        val dxe = d.xor(e)
        val left = a.or(b.or(e).and(dxe))
        val right = a.xor(bd).and(c.xor(d).or(be))
        return left.xor(right)
    }

    /**
     * Compute the Crypto1 nonlinear filter function on bitsliced odd-half states.
     *
     * Takes 20 bitsliced odd-half bits (indices 0..19) and returns a single
     * [LongVector] where each bit position contains the filter output for
     * the corresponding state.
     *
     * The filter decomposes into:
     * - Layer 1: 5 nibble functions (f20a or f20b) on bits [0..3], [4..7], [8..11], [12..15], [16..19]
     * - Layer 2: 5-input combiner f20c on the 5 nibble outputs
     *
     * The combiner arguments are in reverse nibble order (n4, n3, n2, n1, n0)
     * because the scalar lookup table indexes as: f = n0*16 + n1*8 + n2*4 + n3*2 + n4,
     * making n4 bit 0 (the 'a' argument) and n0 bit 4 (the 'e' argument) of f20c.
     *
     * @param odd Array of 24 [LongVector] values, where odd[i] contains bit i of the odd half
     * @return Filter output vector
     */
    fun filter(odd: Array<LongVector>): LongVector {
        val n0 = f20a(odd[0], odd[1], odd[2], odd[3])
        val n1 = f20b(odd[4], odd[5], odd[6], odd[7])
        val n2 = f20a(odd[8], odd[9], odd[10], odd[11])
        val n3 = f20a(odd[12], odd[13], odd[14], odd[15])
        val n4 = f20b(odd[16], odd[17], odd[18], odd[19])
        // Combiner with reversed nibble order: n4=a(bit0), n3=b(bit1), n2=c(bit2), n1=d(bit3), n0=e(bit4)
        return f20c(n4, n3, n2, n1, n0)
    }

    // ---- Bitsliced LFSR state ----

    /**
     * Bitsliced Crypto1 state holding N = [BATCH_SIZE] states in parallel.
     *
     * Each of the 24 even bits and 24 odd bits is stored as a [LongVector].
     * Bit i of lane j in even[k] represents bit k of the even half of state (j * 64 + bit_position_in_lane).
     */
    class BitslicedState(
        val even: Array<LongVector>,
        val odd: Array<LongVector>,
    ) {
        companion object {
            /**
             * Pack an array of scalar states into a bitsliced representation.
             *
             * States are packed so that state index `i` maps to:
             * - Lane: i / 64
             * - Bit within lane: i % 64
             *
             * @param evenStates Array of even half-state values (up to [BATCH_SIZE])
             * @param oddStates Array of odd half-state values (up to [BATCH_SIZE])
             * @return Packed bitsliced state
             */
            fun pack(
                evenStates: IntArray,
                oddStates: IntArray,
            ): BitslicedState {
                require(evenStates.size == oddStates.size) { "Even and odd arrays must have same size" }
                val n = evenStates.size
                require(n <= BATCH_SIZE) { "Cannot pack more than $BATCH_SIZE states" }

                val numLanes = species.length()
                val evenBits = Array(24) { LongArray(numLanes) }
                val oddBits = Array(24) { LongArray(numLanes) }

                for (i in 0 until n) {
                    val lane = i / 64
                    val bit = i % 64
                    val mask = 1L shl bit
                    val ev = evenStates[i]
                    val od = oddStates[i]
                    for (b in 0 until 24) {
                        if ((ev shr b) and 1 != 0) {
                            evenBits[b][lane] = evenBits[b][lane] or mask
                        }
                        if ((od shr b) and 1 != 0) {
                            oddBits[b][lane] = oddBits[b][lane] or mask
                        }
                    }
                }

                val even = Array(24) { LongVector.fromArray(species, evenBits[it], 0) }
                val odd = Array(24) { LongVector.fromArray(species, oddBits[it], 0) }
                return BitslicedState(even, odd)
            }

            /**
             * Pack only even half-states into a bitsliced representation.
             * Odd halves are left as zeros.
             */
            fun packEven(evenStates: IntArray): Array<LongVector> {
                val n = evenStates.size
                require(n <= BATCH_SIZE) { "Cannot pack more than $BATCH_SIZE states" }

                val numLanes = species.length()
                val evenBits = Array(24) { LongArray(numLanes) }

                for (i in 0 until n) {
                    val lane = i / 64
                    val bit = i % 64
                    val mask = 1L shl bit
                    val ev = evenStates[i]
                    for (b in 0 until 24) {
                        if ((ev shr b) and 1 != 0) {
                            evenBits[b][lane] = evenBits[b][lane] or mask
                        }
                    }
                }

                return Array(24) { LongVector.fromArray(species, evenBits[it], 0) }
            }
        }

        /**
         * Extract a single scalar state from the bitsliced representation.
         *
         * @param index State index (0 until [BATCH_SIZE])
         * @return Pair of (even, odd) half-state values
         */
        fun unpackState(index: Int): Pair<UInt, UInt> {
            val lane = index / 64
            val bit = index % 64
            var ev = 0u
            var od = 0u
            val tmpEven = LongArray(species.length())
            val tmpOdd = LongArray(species.length())
            for (b in 0 until 24) {
                even[b].intoArray(tmpEven, 0)
                odd[b].intoArray(tmpOdd, 0)
                if ((tmpEven[lane] shr bit) and 1L != 0L) {
                    ev = ev or (1u shl b)
                }
                if ((tmpOdd[lane] shr bit) and 1L != 0L) {
                    od = od or (1u shl b)
                }
            }
            return Pair(ev, od)
        }
    }

    // ---- LFSR parity (XOR reduction over polynomial taps) ----

    /**
     * Compute XOR of odd-half bits at LF_POLY_ODD tap positions.
     */
    private fun parityOdd(odd: Array<LongVector>): LongVector {
        var result = odd[LF_POLY_ODD_TAPS[0]]
        for (i in 1 until LF_POLY_ODD_TAPS.size) {
            result = result.xor(odd[LF_POLY_ODD_TAPS[i]])
        }
        return result
    }

    /**
     * Compute XOR of even-half bits at LF_POLY_EVEN tap positions.
     */
    private fun parityEven(even: Array<LongVector>): LongVector {
        var result = even[LF_POLY_EVEN_TAPS[0]]
        for (i in 1 until LF_POLY_EVEN_TAPS.size) {
            result = result.xor(even[LF_POLY_EVEN_TAPS[i]])
        }
        return result
    }

    // ---- LFSR rollback ----

    /**
     * Roll back one LFSR bit (reverse one clock step) on bitsliced state.
     *
     * Returns the filter output at the recovered state.
     *
     * Port of Crypto1State.lfsrRollbackBit() to bitsliced form:
     * 1. Mask odd to 24 bits (implicit — we always keep 24 bits)
     * 2. Swap odd and even
     * 3. Extract LSB of even (the bit that was shifted in)
     * 4. Shift even right by 1
     * 5. Compute feedback from polynomial taps and input
     * 6. If encrypted, XOR feedback with filter output
     * 7. Set even[23] = parity(feedback)
     *
     * @param even Mutable even-half bit array
     * @param odd Mutable odd-half bit array
     * @param inputBit Broadcast input bit (ONES or ZEROS)
     * @param isEncrypted Whether to XOR filter output into feedback
     * @return Filter output vector at the recovered state
     */
    fun lfsrRollbackBit(
        even: Array<LongVector>,
        odd: Array<LongVector>,
        inputBit: LongVector,
        isEncrypted: Boolean,
    ): LongVector {
        // Swap odd and even (3-way XOR swap)
        for (i in 0 until 24) {
            val tmp = odd[i]
            odd[i] = even[i]
            even[i] = tmp
        }

        // Extract LSB of even (bit that was shifted in during forward clock)
        val out = even[0]

        // Shift even right by 1: even[i] = even[i+1], even[23] = 0
        // (Scalar version does even >>= 1 which zeroes bit 23; we must do the same
        // so that parityEven() doesn't read stale data from even[23].)
        for (i in 0 until 23) {
            even[i] = even[i + 1]
        }
        even[23] = ZEROS

        // Compute feedback
        var feedback = out
        feedback = feedback.xor(parityEven(even))
        feedback = feedback.xor(parityOdd(odd))
        feedback = feedback.xor(inputBit)

        // Filter output at recovered state
        val filterOut = filter(odd)

        if (isEncrypted) {
            feedback = feedback.xor(filterOut)
        }

        // Set MSB of even to parity(feedback) — but for single-bit parity,
        // since feedback is already a single-bit-per-position value, the
        // parity of a single bit IS that bit.
        even[23] = feedback

        return filterOut
    }

    /**
     * Roll back one byte (8 bits) on bitsliced state.
     *
     * Processes bits 7 downTo 0, matching Crypto1State.lfsrRollbackByte().
     *
     * @param even Mutable even-half bit array
     * @param odd Mutable odd-half bit array
     * @param inputByte The input byte value (0..255)
     * @param isEncrypted Whether this is an encrypted rollback
     * @return 8-element array of filter outputs (bit 7 first, bit 0 last)
     */
    fun lfsrRollbackByte(
        even: Array<LongVector>,
        odd: Array<LongVector>,
        inputByte: Int,
        isEncrypted: Boolean,
    ): Array<LongVector> {
        val filterOuts = Array(8) { ZEROS }
        for (i in 7 downTo 0) {
            val inputBit = if ((inputByte shr i) and 1 != 0) ONES else ZEROS
            filterOuts[i] = lfsrRollbackBit(even, odd, inputBit, isEncrypted)
        }
        return filterOuts
    }

    // ---- LFSR forward ----

    /**
     * Clock LFSR forward one bit on bitsliced state.
     *
     * Returns the filter output (keystream bit) BEFORE clocking.
     *
     * Port of Crypto1State.lfsrBit() to bitsliced form:
     * 1. Compute filter output from odd half
     * 2. Compute feedback from polynomial taps, input, and (optionally) filter output
     * 3. Shift even left by 1, inserting parity(feedback) at LSB
     * 4. Swap odd and even
     *
     * @param even Mutable even-half bit array
     * @param odd Mutable odd-half bit array
     * @param inputBit Broadcast input bit (ONES or ZEROS)
     * @param isEncrypted Whether to feed filter output back
     * @return Filter output (keystream bit) vector
     */
    fun lfsrForwardBit(
        even: Array<LongVector>,
        odd: Array<LongVector>,
        inputBit: LongVector,
        isEncrypted: Boolean,
    ): LongVector {
        val ret = filter(odd)

        // Compute feedback
        var feedin = if (isEncrypted) ret else ZEROS
        feedin = feedin.xor(inputBit)
        feedin = feedin.xor(parityOdd(odd))
        feedin = feedin.xor(parityEven(even))

        // Shift even left by 1, insert parity(feedback) at LSB
        // Since feedin is already a single bit, parity(feedin) = feedin
        for (i in 23 downTo 1) {
            even[i] = even[i - 1]
        }
        even[0] = feedin

        // Swap odd and even
        for (i in 0 until 24) {
            val tmp = odd[i]
            odd[i] = even[i]
            even[i] = tmp
        }

        return ret
    }

    /**
     * Clock LFSR forward one byte (8 bits) on bitsliced state.
     *
     * Returns the 8 keystream bits packed as an array, plus computes
     * the XOR parity of the keystream byte.
     *
     * @param even Mutable even-half bit array
     * @param odd Mutable odd-half bit array
     * @param inputByte The input byte value (0..255)
     * @param isEncrypted Whether this is encrypted mode
     * @return Pair of (keystream byte bits array[8], keystream byte parity)
     */
    fun lfsrForwardByte(
        even: Array<LongVector>,
        odd: Array<LongVector>,
        inputByte: Int,
        isEncrypted: Boolean,
    ): Pair<Array<LongVector>, LongVector> {
        val ksBits = Array(8) { ZEROS }
        var ksParity = ZEROS
        for (i in 0 until 8) {
            val inputBit = if ((inputByte shr i) and 1 != 0) ONES else ZEROS
            ksBits[i] = lfsrForwardBit(even, odd, inputBit, isEncrypted)
            ksParity = ksParity.xor(ksBits[i])
        }
        return Pair(ksBits, ksParity)
    }

    /**
     * Get the filter output at the current state without clocking.
     * This is the "parity keystream bit" — the preview of the next keystream bit.
     */
    fun filterPreview(odd: Array<LongVector>): LongVector = filter(odd)

    // ---- Parity check batch ----

    /**
     * Check parity for a batch of even states against a single broadcast odd state.
     *
     * This implements the inner loop of the brute force:
     * 1. Roll back 1 byte (encrypted) to recover initial key state
     * 2. Forward 4 bytes checking parity at each byte boundary
     * 3. Early exit if all survivors are eliminated
     *
     * @param broadcastOdd 24-element array of broadcast odd-half bits (single state replicated)
     * @param evenBatch 24-element array of packed even-half bits ([BATCH_SIZE] states)
     * @param rollbackInput Input byte for rollback: (uid>>24) ^ bestFirstByte
     * @param inputBytes 4-element array of uid^encNonce bytes (big-endian, byte 0 first)
     * @param encBytes 4-element array of encrypted nonce bytes
     * @param encParBits 4-element array of encrypted parity bits
     * @return Survivor mask vector — bit is 1 if the corresponding state passed all 4 parity checks
     */
    fun checkParityBatchPreTransposed(
        broadcastOdd: Array<LongVector>,
        evenBatch: Array<LongVector>,
        rollbackInput: Int,
        inputBytes: IntArray,
        encBytes: IntArray,
        encParBits: IntArray,
    ): LongVector {
        // Make mutable copies for rollback/forward
        val even = Array(24) { evenBatch[it] }
        val odd = Array(24) { broadcastOdd[it] }

        // Step 1: Roll back 1 byte (encrypted)
        lfsrRollbackByte(even, odd, rollbackInput, true)

        // Step 2: Forward 4 bytes, checking parity after each
        var survivors = ONES

        for (byteIdx in 0 until 4) {
            // Forward 1 byte
            val (ksBits, _) = lfsrForwardByte(even, odd, inputBytes[byteIdx], true)

            // Get parity keystream bit (filter preview after processing this byte)
            val ksParBit = filterPreview(odd)

            // Compute plaintext byte bits: plain = enc XOR ks
            // Then compute XOR parity of plaintext byte
            val encByte = encBytes[byteIdx]
            var plainParity = ZEROS
            for (bitIdx in 0 until 8) {
                val encBit = if ((encByte shr bitIdx) and 1 != 0) ONES else ZEROS
                val plainBit = encBit.xor(ksBits[bitIdx])
                plainParity = plainParity.xor(plainBit)
            }

            // ISO 14443-3A odd parity: 1 when even popcount
            val oddParity = plainParity.inv()

            // Expected encrypted parity: oddParity XOR ksParBit
            val expectedEncPar = oddParity.xor(ksParBit)

            // Actual encrypted parity (broadcast)
            val actualEncPar = if (encParBits[byteIdx] != 0) ONES else ZEROS

            // Survivors: must match (XNOR = NOT XOR)
            val match = expectedEncPar.xor(actualEncPar).inv()
            survivors = survivors.and(match)

            // Early exit
            if (survivors.reduceLanes(VectorOperators.OR) == 0L) {
                return ZEROS
            }
        }

        return survivors
    }

    /**
     * Pre-transpose even states for efficient batched parity checking.
     *
     * Converts an IntArray of even half-states into a 24-element array of
     * LongVector arrays, where result[bitPos][batchIdx] contains the packed
     * bits for that batch.
     *
     * @param evenStates Array of even half-state values
     * @return Array of 24 arrays, each containing ceil(evenStates.size / BATCH_SIZE) LongVectors
     */
    fun preTransposeEvenStates(evenStates: IntArray): Array<Array<LongVector>> {
        val numBatches = (evenStates.size + BATCH_SIZE - 1) / BATCH_SIZE
        val numLanes = species.length()
        val result = Array(24) { Array(numBatches) { ZEROS } }

        for (batchIdx in 0 until numBatches) {
            val batchStart = batchIdx * BATCH_SIZE
            val batchEnd = minOf(batchStart + BATCH_SIZE, evenStates.size)
            val batchBits = Array(24) { LongArray(numLanes) }

            for (i in batchStart until batchEnd) {
                val localIdx = i - batchStart
                val lane = localIdx / 64
                val bit = localIdx % 64
                val mask = 1L shl bit
                val ev = evenStates[i]
                for (b in 0 until 24) {
                    if ((ev shr b) and 1 != 0) {
                        batchBits[b][lane] = batchBits[b][lane] or mask
                    }
                }
            }

            for (b in 0 until 24) {
                result[b][batchIdx] = LongVector.fromArray(species, batchBits[b], 0)
            }
        }

        return result
    }

    /**
     * Broadcast a single odd state into 24 LongVector values.
     */
    fun broadcastOddState(oddState: Int): Array<LongVector> =
        Array(24) { b ->
            if ((oddState shr b) and 1 != 0) ONES else ZEROS
        }

    /**
     * Extract surviving state indices from a survivor mask vector.
     *
     * @param survivors LongVector survivor mask
     * @return List of state indices (0 until BATCH_SIZE) that have their bit set
     */
    fun extractSurvivors(survivors: LongVector): List<Int> {
        val result = mutableListOf<Int>()
        val lanes = LongArray(species.length())
        survivors.intoArray(lanes, 0)
        for (laneIdx in lanes.indices) {
            var bits = lanes[laneIdx]
            while (bits != 0L) {
                val bitPos = java.lang.Long.numberOfTrailingZeros(bits)
                result.add(laneIdx * 64 + bitPos)
                bits = bits and (bits - 1) // clear lowest set bit
            }
        }
        return result
    }
}

/**
 * SIMD-accelerated brute force engine using bitsliced Crypto1 operations.
 *
 * Pre-transposes even states once, then for each odd state broadcasts it
 * and iterates even batches using [SimdBitslice.checkParityBatchPreTransposed].
 * Survivors are extracted and verified with scalar key extraction + multi-nonce check.
 */
class SimdBruteForceEngine : BruteForceEngine {
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
        val candidates = mutableListOf<Long>()

        // Pre-transpose even states into batched bitsliced form
        val transposedEven = SimdBitslice.preTransposeEvenStates(evenStates)
        val numBatches = transposedEven[0].size
        val batchSize = SimdBitslice.BATCH_SIZE
        var tested = 0L

        for (oi in oddStates.indices) {
            val oddState = oddStates[oi]
            val broadcastOdd = SimdBitslice.broadcastOddState(oddState)

            for (batchIdx in 0 until numBatches) {
                val evenBatch = Array(24) { transposedEven[it][batchIdx] }

                val survivors =
                    SimdBitslice.checkParityBatchPreTransposed(
                        broadcastOdd,
                        evenBatch,
                        rollbackInput,
                        inputBytes,
                        encBytes,
                        encParBits,
                    )

                // Extract and verify survivors
                if (survivors.reduceLanes(VectorOperators.OR) != 0L) {
                    val survivorIndices = SimdBitslice.extractSurvivors(survivors)
                    val survivorBatchStart = batchIdx * batchSize
                    for (localIdx in survivorIndices) {
                        val evenIdx = survivorBatchStart + localIdx
                        if (evenIdx >= evenStates.size) continue

                        // Scalar key extraction: roll back to get the key
                        val state = Crypto1State()
                        state.odd = oddState.toUInt()
                        state.even = evenStates[evenIdx].toUInt()
                        state.lfsrRollbackByte(rollbackInput, true)
                        val candidateKey = state.getKey()

                        if (verifyFn(candidateKey)) {
                            candidates.add(candidateKey)
                        }
                    }
                }

                // Count tested pairs for this batch
                val batchStart = batchIdx * batchSize
                val batchEnd = minOf(batchStart + batchSize, evenStates.size)
                tested += (batchEnd - batchStart).toLong()
            }

            onProgress?.invoke(tested, candidates.size)
        }

        return candidates
    }
}
