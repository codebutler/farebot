/*
 * HardnestedSumProperty.kt
 *
 * Based on Proxmark3's hardnested_tables.c and cmdhfmfhard.c.
 * Original authors: Ïkjas, piwi, and Proxmark3 contributors.
 * https://github.com/RfidResearchGroup/proxmark3
 *
 * Two-level sum property computation for the hardnested attack on MIFARE Classic.
 * Ported to Kotlin Multiplatform for FareBot.
 *
 * The Crypto1 filter function has a biased output distribution. The "partial sum
 * property" for a 20-bit LFSR half-state measures how many of 16 possible 4-bit
 * feedback patterns (from the other half) produce even parity in the filter output
 * sequence. This partitions states into groups.
 *
 * Two levels of sum filtering are applied:
 * - sum_a0: Based on byte 0 of the nonce — uses the TOP 20 bits [23:4] of the
 *   24-bit half-state. The bottom 4 bits are wildcarded.
 * - sum_a8: Based on byte 1 of the nonce — uses the BOTTOM 20 bits [19:0] of the
 *   24-bit half-state. The top 4 bits are wildcarded.
 *
 * Since a0 and a8 overlap in bits [19:4] (16 bits), ANDing both constraints
 * dramatically reduces the candidate space.
 *
 * Reference: Carlo Meijer & Roel Verdult, "Ciphertext-only Cryptanalysis on
 * Hardened Mifare Classic Cards" (ACM CCS 2015)
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
 * Two-level sum property analysis for the hardnested attack.
 *
 * Provides bitarrays for both sum_a0 (byte 0) and sum_a8 (byte 1) constraints,
 * enabling the 4-dimensional (p, q, r, s) candidate generation used by Proxmark3.
 */
object HardnestedSumProperty {
    /**
     * Precomputed filter lookup table: maps 20-bit state to 0/1 filter output.
     * 1M entries x 4 bytes = 4 MB.
     */
    internal val filterLut: IntArray by lazy {
        IntArray(1 shl 20) { Crypto1.filter(it.toUInt()) }
    }

    /**
     * Compute the 4-bit filter output pattern for a 24-bit half-state.
     *
     * Evaluates the filter at 4 consecutive shift positions (simulating
     * 4 LFSR clocks of one half). Returns outputs packed into bits 0..3.
     */
    internal fun filterPattern4(state: Int): Int {
        val lut = filterLut
        return lut[state and 0xFFFFF] or
            (lut[(state ushr 1) and 0xFFFFF] shl 1) or
            (lut[(state ushr 2) and 0xFFFFF] shl 2) or
            (lut[(state ushr 3) and 0xFFFFF] shl 3)
    }

    /**
     * Compute the partial sum property for an ODD half-state (20-bit input).
     *
     * Port of Proxmark3's PartialSumProperty(state, ODD_STATE).
     *
     * For each of 16 possible 4-bit feedback patterns, evaluates the filter
     * function 5 times while shifting and inserting feedback bits. Counts how
     * many patterns produce even parity across the 5 filter outputs.
     *
     * @param state20 20-bit LFSR half-state
     * @return Value in 0..16 (always even)
     */
    internal fun partialSumPropertyOdd(state20: Int): Int {
        val lut = filterLut
        var sum = 0
        for (j in 0 until 16) {
            var st = state20
            var partSum = 0
            for (i in 0 until 5) {
                partSum = partSum xor lut[st and 0xFFFFF]
                st = ((st shl 1) or ((j shr (3 - i)) and 1)) and 0xFFFFFF
            }
            partSum = partSum xor 1
            sum += partSum
        }
        return sum
    }

    /**
     * Compute the partial sum property for an EVEN half-state (20-bit input).
     *
     * Port of Proxmark3's PartialSumProperty(state, EVEN_STATE).
     *
     * Same concept as odd, but shifts BEFORE evaluating (4 evaluations,
     * no final XOR with 1).
     *
     * @param state20 20-bit LFSR half-state
     * @return Value in 0..16 (always even)
     */
    internal fun partialSumPropertyEven(state20: Int): Int {
        val lut = filterLut
        var sum = 0
        for (j in 0 until 16) {
            var st = state20
            var partSum = 0
            for (i in 0 until 4) {
                st = ((st shl 1) or ((j shr (3 - i)) and 1)) and 0xFFFFFF
                partSum = partSum xor lut[st and 0xFFFFF]
            }
            sum += partSum
        }
        return sum
    }

    // ---- Partial sum counts (distribution is the same for a0 and a8) ----

    private val oddSumCounts_: IntArray by lazy {
        val lut = filterLut
        val counts = IntArray(17)
        for (state20 in 0 until (1 shl 20)) {
            val ps = partialSumPropertyOdd(state20)
            counts[ps] += 16 // each 20-bit state maps to 16 24-bit states
        }
        counts
    }

    private val evenSumCounts_: IntArray by lazy {
        val lut = filterLut
        val counts = IntArray(17)
        for (state20 in 0 until (1 shl 20)) {
            val ps = partialSumPropertyEven(state20)
            counts[ps] += 16
        }
        counts
    }

    val oddSumCounts: IntArray get() = oddSumCounts_
    val evenSumCounts: IntArray get() = evenSumCounts_

    // ---- part_sum_a0 bitarrays: top 20 bits [23:4] determine property ----

    private val partSumA0Odd_: Array<StateBitarray> by lazy {
        val lut = filterLut
        val arrays = Array(17) { StateBitarray() }
        for (state20 in 0 until (1 shl 20)) {
            val ps = partialSumPropertyOdd(state20)
            for (lowBits in 0 until 16) {
                arrays[ps].set((state20 shl 4) or lowBits)
            }
        }
        arrays
    }

    private val partSumA0Even_: Array<StateBitarray> by lazy {
        val lut = filterLut
        val arrays = Array(17) { StateBitarray() }
        for (state20 in 0 until (1 shl 20)) {
            val ps = partialSumPropertyEven(state20)
            for (lowBits in 0 until 16) {
                arrays[ps].set((state20 shl 4) or lowBits)
            }
        }
        arrays
    }

    val partSumA0OddBitarrays: Array<StateBitarray> get() = partSumA0Odd_
    val partSumA0EvenBitarrays: Array<StateBitarray> get() = partSumA0Even_

    // ---- part_sum_a8 bitarrays: bottom 20 bits [19:0] determine property ----

    private val partSumA8Odd_: Array<StateBitarray> by lazy {
        val lut = filterLut
        val arrays = Array(17) { StateBitarray() }
        for (state20 in 0 until (1 shl 20)) {
            val ps = partialSumPropertyOdd(state20)
            for (highBits in 0 until 16) {
                arrays[ps].set(state20 or (highBits shl 20))
            }
        }
        arrays
    }

    private val partSumA8Even_: Array<StateBitarray> by lazy {
        val lut = filterLut
        val arrays = Array(17) { StateBitarray() }
        for (state20 in 0 until (1 shl 20)) {
            val ps = partialSumPropertyEven(state20)
            for (highBits in 0 until 16) {
                arrays[ps].set(state20 or (highBits shl 20))
            }
        }
        arrays
    }

    val partSumA8OddBitarrays: Array<StateBitarray> get() = partSumA8Odd_
    val partSumA8EvenBitarrays: Array<StateBitarray> get() = partSumA8Even_

    // ---- Combined sum property (Proxmark3's SumProperty) ----

    /**
     * The 19 possible values of the combined SumProperty.
     *
     * SumProperty = P * (16 - Q) + (16 - P) * Q, where P and Q are partial sums
     * (always even, range 0..16). Since P and Q are even, SumProperty values are
     * restricted to these 19 values.
     */
    val SUMS = intArrayOf(0, 32, 56, 64, 80, 96, 104, 112, 120, 128, 136, 144, 152, 160, 176, 192, 200, 224, 256)

    /**
     * Compute the combined SumProperty from odd and even partial sums.
     *
     * SumProperty(P, Q) = P * (16 - Q) + (16 - P) * Q
     *
     * This formula counts how many of the 256 possible 8-bit nonce patterns
     * produce even keystream byte parity, given the odd partial sum P and
     * even partial sum Q.
     */
    fun combinedSum(
        oddPartialSum: Int,
        evenPartialSum: Int,
    ): Int = oddPartialSum * (16 - evenPartialSum) + (16 - oddPartialSum) * evenPartialSum

    /**
     * Prior probabilities for each of the 19 sum values.
     *
     * Computed from the product of odd and even partial sum count distributions.
     */
    fun sumProbabilities(): DoubleArray {
        val total24 = (1 shl 24).toDouble()
        val probs = DoubleArray(SUMS.size)

        for (p in 0..16) {
            if (oddSumCounts[p] == 0) continue
            for (q in 0..16) {
                if (evenSumCounts[q] == 0) continue
                val s = combinedSum(p, q)
                val idx = SUMS.indexOf(s)
                if (idx >= 0) {
                    probs[idx] += (oddSumCounts[p].toDouble() / total24) *
                        (evenSumCounts[q].toDouble() / total24)
                }
            }
        }
        return probs
    }

    /**
     * Enumerate all (oddPartialSum, evenPartialSum) pairs that produce a given
     * combined sum value.
     */
    fun partialSumPairsForCombinedSum(targetSum: Int): List<Pair<Int, Int>> {
        val pairs = mutableListOf<Pair<Int, Int>>()
        for (p in 0..16) {
            if (oddSumCounts[p] == 0) continue
            for (q in 0..16) {
                if (evenSumCounts[q] == 0) continue
                if (combinedSum(p, q) == targetSum) {
                    pairs.add(Pair(p, q))
                }
            }
        }
        return pairs
    }

    /**
     * Estimate sum_a8 probability for a given first-byte bucket using
     * hypergeometric/Bayesian inference.
     *
     * Given that we observed [successes] "even parity" events out of [total]
     * nonces with a particular first byte, estimate the probability that
     * the true sum_a8 is each of the 19 possible values.
     *
     * @return List of (sumValue, probability) sorted by descending probability
     */
    fun estimateSumA8(
        total: Int,
        successes: Int,
    ): List<Pair<Int, Double>> {
        if (total == 0) return emptyList()

        val priors = sumProbabilities()
        val posteriors = DoubleArray(SUMS.size)

        for (i in SUMS.indices) {
            // Expected success rate for this sum value: sum / 256
            val expectedRate = SUMS[i].toDouble() / 256.0
            // Binomial log-likelihood (unnormalized)
            posteriors[i] = priors[i] * binomialProbability(total, successes, expectedRate)
        }

        // Normalize
        val totalProb = posteriors.sum()
        if (totalProb > 0) {
            for (i in posteriors.indices) posteriors[i] /= totalProb
        }

        return SUMS.indices
            .map { Pair(SUMS[it], posteriors[it]) }
            .filter { it.second > 0.0001 }
            .sortedByDescending { it.second }
    }

    /**
     * Binomial probability P(X=k | n, p) using log-space to avoid overflow.
     */
    private fun binomialProbability(
        n: Int,
        k: Int,
        p: Double,
    ): Double {
        if (p <= 0.0) return if (k == 0) 1.0 else 0.0
        if (p >= 1.0) return if (k == n) 1.0 else 0.0

        var logProb = 0.0
        // log(C(n,k)) + k*log(p) + (n-k)*log(1-p)
        for (i in 0 until k) {
            logProb += kotlin.math.ln((n - i).toDouble() / (i + 1).toDouble())
        }
        logProb += k * kotlin.math.ln(p) + (n - k) * kotlin.math.ln(1 - p)
        return kotlin.math.exp(logProb)
    }

    // ---- Compact accessors (Proxmark3 convention: index i = partial sum 2*i) ----

    private val partSumA0Compact_: Array<Array<StateBitarray>> by lazy {
        arrayOf(
            Array(NUM_PART_SUMS) { partSumA0Even_[it * 2] }, // EVEN_STATE = 0
            Array(NUM_PART_SUMS) { partSumA0Odd_[it * 2] }, // ODD_STATE = 1
        )
    }

    private val partSumA8Compact_: Array<Array<StateBitarray>> by lazy {
        arrayOf(
            Array(NUM_PART_SUMS) { partSumA8Even_[it * 2] }, // EVEN_STATE = 0
            Array(NUM_PART_SUMS) { partSumA8Odd_[it * 2] }, // ODD_STATE = 1
        )
    }

    /**
     * Compact part_sum_a0 bitarrays indexed [0..NUM_PART_SUMS-1].
     * Index i corresponds to partial sum value 2*i.
     * Port of Proxmark3's part_sum_a0_bitarrays[odd_even].
     */
    fun partSumA0Bitarrays(oddEven: Int): Array<StateBitarray> = partSumA0Compact_[oddEven]

    /**
     * Compact part_sum_a8 bitarrays indexed [0..NUM_PART_SUMS-1].
     * Index i corresponds to partial sum value 2*i.
     * Port of Proxmark3's part_sum_a8_bitarrays[odd_even].
     */
    fun partSumA8Bitarrays(oddEven: Int): Array<StateBitarray> = partSumA8Compact_[oddEven]

    // ---- sum_a0 bitarrays (port of init_sum_bitarrays) ----

    /**
     * sum_a0_bitarrays[oddEven][sumIdx]: union of part_sum_a0 bitarrays whose
     * combined sum equals SUMS[sumIdx].
     *
     * Port of Proxmark3's init_sum_bitarrays() lines 656-677.
     * For each (p, q) pair where 2*p*(16-2*q) + (16-2*p)*2*q == SUMS[sumIdx]:
     *   sum_a0_bitarrays[EVEN][sumIdx] |= part_sum_a0[EVEN][q]
     *   sum_a0_bitarrays[ODD][sumIdx] |= part_sum_a0[ODD][p]
     */
    private val sumA0_: Array<Array<StateBitarray>> by lazy {
        val compact = partSumA0Compact_ // force init
        val arrays =
            arrayOf(
                Array(NUM_SUMS) { StateBitarray() }, // EVEN_STATE
                Array(NUM_SUMS) { StateBitarray() }, // ODD_STATE
            )
        for (p in 0 until NUM_PART_SUMS) {
            for (q in 0 until NUM_PART_SUMS) {
                val sumA0 = 2 * p * (16 - 2 * q) + (16 - 2 * p) * 2 * q
                var sumA0Idx = 0
                while (SUMS[sumA0Idx] != sumA0) sumA0Idx++
                arrays[EVEN_STATE][sumA0Idx].or(compact[EVEN_STATE][q])
                arrays[ODD_STATE][sumA0Idx].or(compact[ODD_STATE][p])
            }
        }
        arrays
    }

    /**
     * Get sum_a0 bitarrays for the given odd/even state.
     * Indexed by sum index (0..NUM_SUMS-1), corresponding to SUMS[i].
     * Constrains the TOP 20 bits [23:4] of the half-state.
     */
    fun sumA0Bitarrays(oddEven: Int): Array<StateBitarray> = sumA0_[oddEven]

    // ---- sum_a8 bitarrays (same structure as sum_a0, but for bottom 20 bits) ----

    /**
     * sum_a8_bitarrays[oddEven][sumIdx]: union of part_sum_a8 bitarrays whose
     * combined sum equals SUMS[sumIdx].
     *
     * Same construction as sum_a0 but using part_sum_a8 (bottom 20 bits).
     */
    private val sumA8_: Array<Array<StateBitarray>> by lazy {
        val compact = partSumA8Compact_ // force init
        val arrays =
            arrayOf(
                Array(NUM_SUMS) { StateBitarray() }, // EVEN_STATE
                Array(NUM_SUMS) { StateBitarray() }, // ODD_STATE
            )
        for (p in 0 until NUM_PART_SUMS) {
            for (q in 0 until NUM_PART_SUMS) {
                val sumA8 = 2 * p * (16 - 2 * q) + (16 - 2 * p) * 2 * q
                var sumA8Idx = 0
                while (SUMS[sumA8Idx] != sumA8) sumA8Idx++
                arrays[EVEN_STATE][sumA8Idx].or(compact[EVEN_STATE][q])
                arrays[ODD_STATE][sumA8Idx].or(compact[ODD_STATE][p])
            }
        }
        arrays
    }

    /**
     * Get sum_a8 bitarrays for the given odd/even state.
     * Indexed by sum index (0..NUM_SUMS-1), corresponding to SUMS[i].
     * Constrains the BOTTOM 20 bits [19:0] of the half-state.
     */
    fun sumA8Bitarrays(oddEven: Int): Array<StateBitarray> = sumA8_[oddEven]

    // ---- Convenience accessors ----

    val oddSumBitarrays: Array<StateBitarray> get() = partSumA8OddBitarrays
    val evenSumBitarrays: Array<StateBitarray> get() = partSumA8EvenBitarrays

    fun bitarrayForPartialSum(
        partialSum: Int,
        isOdd: Boolean,
    ): StateBitarray {
        val arrays = if (isOdd) oddSumBitarrays else evenSumBitarrays
        return arrays[partialSum].copy()
    }

    const val EVEN_STATE = 0
    const val ODD_STATE = 1
    const val NUM_PART_SUMS = 9
    const val NUM_SUMS = 19
}
