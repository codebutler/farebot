/*
 * HardnestedAttack.kt
 *
 * Based on Proxmark3's cmdhfmfhard.c hardnested attack implementation.
 * Original authors: Ïkjas, piwi, and Proxmark3 contributors.
 * https://github.com/RfidResearchGroup/proxmark3
 *
 * Ported to Kotlin Multiplatform for FareBot.
 *
 * Algorithm:
 * Phase 1: Collect encrypted nonces with parity bits until all 256 first bytes seen
 * Phase 2: Apply bitflip tables per-first-byte, estimate sum properties, select best byte
 * Phase 3: Generate candidates and brute force, iterating sum_a8 guesses
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

import com.codebutler.farebot.card.CardLostException
import com.codebutler.farebot.keymanager.pn533.PN533RawClassic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log2
import kotlin.time.TimeSource

/**
 * Hardnested attack for MIFARE Classic key recovery.
 *
 * Faithful port of Proxmark3's cmdhfmfhard.c.
 */
class HardnestedAttack(
    private val rawClassic: PN533RawClassic,
    private val uid: UInt,
) {
    data class NonceData(
        val encryptedNonce: UInt,
        val encryptedParity: Int,
    )

    // ---- Per-first-byte nonce data (port of Proxmark3's noncelist_t nonces[256]) ----

    /**
     * Entry in the per-first-byte nonce list. Sorted by 2nd byte, deduplicated.
     * Port of noncelistentry_t.
     */
    private class NonceEntry(
        val nonceEnc: UInt,
        val parEnc: Int,
    )

    /**
     * Per-first-byte data structure.
     * Port of noncelist_t.
     */
    private class NonceList {
        val entries = mutableListOf<NonceEntry>()
        var num: Int = 0 // count of distinct 2nd bytes
        var sum: Int = 0 // sum of evenparity(byte1 | par1) for distinct 2nd bytes
        var sumA8GuessDirty = false

        /** sum_a8 guesses: array of (sum_a8_idx, probability), sorted by prob desc. */
        val sumA8Guess = Array(NUM_SUMS) { SumA8Guess(sumA8Idx = it) }

        var expectedNumBruteForce: Float = 0f

        /** Per-first-byte bitflip-filtered state bitarrays [EVEN=0, ODD=1]. */
        val statesBitarray = arrayOf(StateBitarray(), StateBitarray())
        val numStatesBitarray = intArrayOf(1 shl 24, 1 shl 24)

        /** Tracks which bitflip values have been applied to this first byte. */
        val bitFlips = BooleanArray(0x400)

        /** Dirty flags for all_bitflips_bitarray update. */
        val allBitflipsDirty = booleanArrayOf(false, false)

        init {
            statesBitarray[EVEN_STATE].setAll()
            statesBitarray[ODD_STATE].setAll()
        }
    }

    private class SumA8Guess(
        var sumA8Idx: Int = 0,
        var prob: Float = 0f,
        var numStates: Long = 0,
    )

    private val nonceLists = Array(256) { NonceList() }
    private var firstByteNum = 0
    private var firstByteSum = 0

    /** Global bitflip intersection arrays [EVEN=0, ODD=1]. */
    private val allBitflipsBitarray = arrayOf(StateBitarray(), StateBitarray())
    private val numAllBitflipsBitarray = intArrayOf(1 shl 24, 1 shl 24)
    private val allBitflipsBitarrayDirty = booleanArrayOf(false, false)

    /** Best first bytes array (sorted by expected brute force). */
    private val bestFirstBytes = IntArray(256) { it }

    /** Dirty flag for p_K update — set when sum bitarrays change. */
    private var pKDirty = false

    init {
        allBitflipsBitarray[EVEN_STATE].setAll()
        allBitflipsBitarray[ODD_STATE].setAll()
    }

    /**
     * Add a nonce, deduplicating by (first_byte, second_byte).
     * Port of add_nonce() from cmdhfmfhard.c lines 716-766.
     */
    private fun addNonce(
        nonceEnc: UInt,
        parEnc: Int,
    ): Int {
        val firstByte = (nonceEnc shr 24).toInt() and 0xFF
        val secondByte = (nonceEnc shr 16).toInt() and 0xFF
        val nl = nonceLists[firstByte]

        if (nl.entries.isEmpty()) {
            // First nonce with this 1st byte
            firstByteNum++
            // evenparity32((nonce_enc & 0xff000000) | (par_enc & 0x08))
            // par_enc bit 3 = parity of byte 0
            firstByteSum +=
                evenparity32(
                    (nonceEnc.toInt() and 0xFF000000.toInt()) or (parEnc and 0x08),
                )
        }

        // Check if we already have this 2nd byte (deduplication)
        for (entry in nl.entries) {
            if (((entry.nonceEnc shr 16).toInt() and 0xFF) == secondByte) {
                return 0 // already seen this (byte0, byte1) pair
            }
        }

        // Add new entry
        nl.entries.add(NonceEntry(nonceEnc, parEnc))
        nl.num++
        // evenparity32((nonce_enc & 0x00ff0000) | (par_enc & 0x04))
        nl.sum +=
            evenparity32(
                (nonceEnc.toInt() and 0x00FF0000) or (parEnc and 0x04),
            )
        nl.sumA8GuessDirty = true
        return 1
    }

    /**
     * evenparity32: returns 1 if popcount is odd (XOR of all bits).
     * Port of Proxmark3's evenparity32() which uses __builtin_parity (GCC),
     * returning 1 for odd popcount despite the name "even".
     */
    private fun evenparity32(x: Int): Int = x.countOneBits() and 1

    // ---- Sum property estimation (port of sum_probability / estimate_sum_a8) ----

    /**
     * Hypergeometric probability P(X=k | N=256, K=sums[i_K], n, k).
     * Port of p_hypergeometric() from cmdhfmfhard.c lines 821-867.
     */
    private fun pHypergeometric(
        iK: Int,
        n: Int,
        k: Int,
    ): Double {
        val bigN = 256
        val bigK = SUMS[iK]

        if (n - k > bigN - bigK || k > bigK) return 0.0

        if (k == 0) {
            var logResult = 0.0
            for (i in (bigN - bigK) downTo (bigN - bigK - n + 1)) {
                logResult += ln(i.toDouble())
            }
            for (i in bigN downTo (bigN - n + 1)) {
                logResult -= ln(i.toDouble())
            }
            return exp(logResult)
        } else {
            if (n - k == bigN - bigK) {
                var logResult = 0.0
                for (i in (k + 1)..n) {
                    if (i != 0) logResult += ln(i.toDouble())
                }
                for (i in (bigK + 1)..bigN) {
                    if (i != 0) logResult -= ln(i.toDouble())
                }
                return exp(logResult)
            } else {
                return pHypergeometric(iK, n, k - 1) *
                    (bigK - k + 1).toDouble() * (n - k + 1).toDouble() /
                    (k.toDouble() * (bigN - bigK - n + k).toDouble())
            }
        }
    }

    /**
     * Posterior probability P(S=K | T=k, n) using Bayesian inference with hypergeometric model.
     * Port of sum_probability() from cmdhfmfhard.c lines 869-881.
     */
    private fun sumProbability(
        iK: Int,
        n: Int,
        k: Int,
        pK: FloatArray,
    ): Float {
        if (k > SUMS[iK]) return 0f

        val pTIsKWhenSIsK = pHypergeometric(iK, n, k)
        val pSIsK = pK[iK].toDouble()
        var pTIsK = 0.0
        for (i in 0 until NUM_SUMS) {
            pTIsK += pK[i].toDouble() * pHypergeometric(i, n, k)
        }
        return if (pTIsK > 0) (pTIsKWhenSIsK * pSIsK / pTIsK).toFloat() else 0f
    }

    /**
     * Estimate sum_a8 for each first byte that has dirty data.
     * Port of estimate_sum_a8() from cmdhfmfhard.c lines 1222-1235.
     */
    private fun estimateSumA8(pK: FloatArray) {
        if (firstByteNum != 256) return
        for (i in 0 until 256) {
            val nl = nonceLists[i]
            if (!nl.sumA8GuessDirty) continue
            for (j in 0 until NUM_SUMS) {
                val sumA8Idx = nl.sumA8Guess[j].sumA8Idx
                nl.sumA8Guess[j].prob = sumProbability(sumA8Idx, nl.num, nl.sum, pK)
            }
            nl.sumA8Guess.sortByDescending { it.prob }
            nl.sumA8GuessDirty = false
        }
    }

    // ---- Bitflip property checking (port of check_for_BitFlipProperties) ----

    /**
     * Apply bitflip tables per-first-byte.
     * Port of check_for_BitFlipProperties_thread() lines 1320-1410.
     * Only handles 1st byte bitflips (CHECK_1ST_BYTES).
     */
    private fun checkForBitFlipProperties() {
        for (bitflipIdx in 0 until num1stByteEffectiveBitflips) {
            val bitflip = allEffectiveBitflip[bitflipIdx]
            for (i in 0 until 256) {
                val nl = nonceLists[i]
                if (nl.bitFlips[bitflip] || nl.bitFlips[bitflip xor 0x100]) continue
                if (nl.entries.isEmpty()) continue
                val jByte = i xor (bitflip and 0xFF)
                if (nonceLists[jByte].entries.isEmpty()) continue

                val parity1 = (nl.entries[0].parEnc shr 3) and 1
                val parity2 = (nonceLists[jByte].entries[0].parEnc shr 3) and 1

                val match =
                    (parity1 == parity2 && (bitflip and 0x100) == 0) ||
                        (parity1 != parity2 && (bitflip and 0x100) != 0)

                if (match) {
                    nl.bitFlips[bitflip] = true
                    for (oddEven in EVEN_STATE..ODD_STATE) {
                        val table = bitflipBitarrays[oddEven][bitflip] ?: continue
                        val oldCount = nl.numStatesBitarray[oddEven]
                        nl.statesBitarray[oddEven].and(table)
                        nl.numStatesBitarray[oddEven] = nl.statesBitarray[oddEven].popcount()
                        if (nl.numStatesBitarray[oddEven] != oldCount) {
                            nl.allBitflipsDirty[oddEven] = true
                        }
                    }
                }
            }
        }
    }

    /**
     * Update the global all_bitflips_bitarray from dirty per-first-byte arrays.
     * Port of update_allbitflips_array() lines 898-912.
     *
     * Uses count_bitarray_low20_AND (coarse AND at 16-bit granularity) matching Proxmark3.
     */
    private fun updateAllBitflipsArray() {
        for (i in 0 until 256) {
            for (oddEven in EVEN_STATE..ODD_STATE) {
                if (nonceLists[i].allBitflipsDirty[oddEven]) {
                    val oldCount = numAllBitflipsBitarray[oddEven]
                    numAllBitflipsBitarray[oddEven] =
                        allBitflipsBitarray[oddEven].andLow20(nonceLists[i].statesBitarray[oddEven])
                    nonceLists[i].allBitflipsDirty[oddEven] = false
                    if (numAllBitflipsBitarray[oddEven] != oldCount) {
                        allBitflipsBitarrayDirty[oddEven] = true
                    }
                }
            }
        }
    }

    /**
     * Update part_sum bitarrays by ANDing with all_bitflips_bitarray (if dirty).
     * Port of update_sum_bitarrays() lines 999-1016.
     */
    private fun updateSumBitarrays(oddEven: Int) {
        if (allBitflipsBitarrayDirty[oddEven]) {
            // Note: Proxmark3 modifies the global part_sum_a0/a8_bitarrays in-place here,
            // but those are re-initialized via init_part_sum_bitarrays() at the start of
            // each attack. Our HardnestedSumProperty uses lazy singletons that don't
            // re-initialize, so we must NOT modify them — doing so would corrupt subsequent
            // sector attacks. The part_sum bitarrays are correct without this pre-filtering;
            // generateCandidates applies allBitflipsBitarray via allBitflipsMatch().
            for (i in 0 until 256) {
                nonceLists[i].statesBitarray[oddEven].and(allBitflipsBitarray[oddEven])
                nonceLists[i].numStatesBitarray[oddEven] =
                    nonceLists[i].statesBitarray[oddEven].popcount()
            }
            pKDirty = true
            allBitflipsBitarrayDirty[oddEven] = false
        }
    }

    /**
     * Apply sum_a0 constraint to all_bitflips_bitarray.
     * Port of apply_sum_a0() lines 1460-1471.
     *
     * Byte 0 observation → sum_a0 → sum_a0_bitarrays (top 20 bits [23:4]).
     * Matches Proxmark3's apply_sum_a0() exactly.
     */
    private fun applySumA0() {
        for (oddEven in EVEN_STATE..ODD_STATE) {
            val oldCount = numAllBitflipsBitarray[oddEven]
            allBitflipsBitarray[oddEven].and(
                HardnestedSumProperty.sumA0Bitarrays(oddEven)[firstByteSum],
            )
            numAllBitflipsBitarray[oddEven] = allBitflipsBitarray[oddEven].popcount()
            if (numAllBitflipsBitarray[oddEven] != oldCount) {
                allBitflipsBitarrayDirty[oddEven] = true
            }
        }
    }

    // ---- Best first byte selection (port of sort_best_first_bytes) ----

    /**
     * Coarse estimate of candidate states for a (sum_a0, sum_a8) pair.
     * Port of estimated_num_states_coarse() lines 959-976.
     *
     * Bitarray mapping (matches Proxmark3 exactly):
     * p,q (from sum_a0/byte 0) → part_sum_a0 bitarrays (top 20 bits [23:4]);
     * r,s (from sum_a8/byte 1) → part_sum_a8 bitarrays (bottom 20 bits [19:0]).
     */
    private fun estimatedNumStatesCoarse(
        sumA0: Int,
        sumA8: Int,
    ): Long {
        var numStates = 0L
        for (p in 0 until NUM_PART_SUMS) {
            for (q in 0 until NUM_PART_SUMS) {
                if (2 * p * (16 - 2 * q) + (16 - 2 * p) * 2 * q != sumA0) continue
                for (r in 0 until NUM_PART_SUMS) {
                    for (s in 0 until NUM_PART_SUMS) {
                        if (2 * r * (16 - 2 * s) + (16 - 2 * r) * 2 * s != sumA8) continue
                        val oddCount =
                            HardnestedSumProperty
                                .partSumA0Bitarrays(ODD_STATE)[p]
                                .andPopcount(HardnestedSumProperty.partSumA8Bitarrays(ODD_STATE)[r])
                        val evenCount =
                            HardnestedSumProperty
                                .partSumA0Bitarrays(EVEN_STATE)[q]
                                .andPopcount(HardnestedSumProperty.partSumA8Bitarrays(EVEN_STATE)[s])
                        numStates += oddCount.toLong() * evenCount.toLong()
                    }
                }
            }
        }
        return numStates
    }

    /**
     * Update p_K based on estimated state counts per sum_a8 value.
     * Port of update_p_K() lines 978-997.
     */
    private fun updatePK(pK: FloatArray) {
        var totalCount = 0L
        val sumA0 = SUMS[firstByteSum]
        for (sumA8Idx in 0 until NUM_SUMS) {
            totalCount += estimatedNumStatesCoarse(sumA0, SUMS[sumA8Idx])
        }
        if (totalCount > 0) {
            for (sumA8Idx in 0 until NUM_SUMS) {
                pK[sumA8Idx] = estimatedNumStatesCoarse(sumA0, SUMS[sumA8Idx]).toFloat() / totalCount.toFloat()
            }
        }
    }

    /**
     * Sort best first bytes by expected brute force.
     * Port of sort_best_first_bytes() lines 1073-1159.
     */
    private fun sortBestFirstBytes(pK: FloatArray): Float {
        // Initialize: rough estimation for each first byte
        for (i in 0 until 256) {
            bestFirstBytes[i] = i
            var probAllFailed = 1f
            nonceLists[i].expectedNumBruteForce = 0f
            for (j in 0 until NUM_SUMS) {
                nonceLists[i].sumA8Guess[j].numStates =
                    estimatedNumStatesCoarse(SUMS[firstByteSum], SUMS[nonceLists[i].sumA8Guess[j].sumA8Idx])
                nonceLists[i].expectedNumBruteForce +=
                    nonceLists[i].sumA8Guess[j].prob * nonceLists[i].sumA8Guess[j].numStates.toFloat() / 2f
                probAllFailed -= nonceLists[i].sumA8Guess[j].prob
                nonceLists[i].expectedNumBruteForce +=
                    probAllFailed * nonceLists[i].sumA8Guess[j].numStates.toFloat() / 2f
            }
        }

        // Sort by expected brute force ascending
        val sorted = bestFirstBytes.toMutableList()
        sorted.sortBy { nonceLists[it].expectedNumBruteForce }
        sorted.forEachIndexed { idx, v -> bestFirstBytes[idx] = v }

        // Refine top candidate (NUM_REFINES = 1 in Proxmark3)
        val firstByte = bestFirstBytes[0]
        for (j in 0 until NUM_SUMS) {
            if (nonceLists[firstByte].sumA8Guess[j].prob <= 0.05f) break
            nonceLists[firstByte].sumA8Guess[j].numStates =
                estimatedNumStates(firstByte, SUMS[firstByteSum], SUMS[nonceLists[firstByte].sumA8Guess[j].sumA8Idx])
        }
        var probAllFailed = 1f
        nonceLists[firstByte].expectedNumBruteForce = 0f
        for (j in 0 until NUM_SUMS) {
            nonceLists[firstByte].expectedNumBruteForce +=
                nonceLists[firstByte].sumA8Guess[j].prob * nonceLists[firstByte].sumA8Guess[j].numStates.toFloat() / 2f
            probAllFailed -= nonceLists[firstByte].sumA8Guess[j].prob
            nonceLists[firstByte].expectedNumBruteForce +=
                probAllFailed * nonceLists[firstByte].sumA8Guess[j].numStates.toFloat() / 2f
        }

        // Find actual best among top 10
        var leastExpected = Float.MAX_VALUE
        var bestIdx = 0
        for (i in 0 until minOf(10, 256)) {
            val fb = bestFirstBytes[i]
            if (nonceLists[fb].expectedNumBruteForce < leastExpected) {
                leastExpected = nonceLists[fb].expectedNumBruteForce
                bestIdx = i
            }
        }
        if (bestIdx != 0) {
            val tmp = bestFirstBytes[0]
            bestFirstBytes[0] = bestFirstBytes[bestIdx]
            bestFirstBytes[bestIdx] = tmp
        }

        return nonceLists[bestFirstBytes[0]].expectedNumBruteForce
    }

    /**
     * Fine-grained estimate using per-first-byte bitflip bitarray.
     * Port of estimated_num_states() lines 940-957 / estimated_num_states_part_sum() lines 919-938.
     *
     * Bitarray mapping (matches Proxmark3 exactly):
     * ODD_STATE: 3-way AND (part_sum_a0[p], part_sum_a8[r], nonces[firstByte].states)
     * EVEN_STATE: 4-way AND (part_sum_a0[q], part_sum_a8[s], nonces[firstByte].states,
     *                        nonces[firstByte ^ 0x80].states)
     */
    private fun estimatedNumStates(
        firstByte: Int,
        sumA0: Int,
        sumA8: Int,
    ): Long {
        var numStates = 0L
        for (p in 0 until NUM_PART_SUMS) {
            for (q in 0 until NUM_PART_SUMS) {
                if (2 * p * (16 - 2 * q) + (16 - 2 * p) * 2 * q != sumA0) continue
                for (r in 0 until NUM_PART_SUMS) {
                    for (s in 0 until NUM_PART_SUMS) {
                        if (2 * r * (16 - 2 * s) + (16 - 2 * r) * 2 * s != sumA8) continue
                        val oddCount =
                            HardnestedSumProperty
                                .partSumA0Bitarrays(ODD_STATE)[p]
                                .threeWayAndPopcount(
                                    HardnestedSumProperty.partSumA8Bitarrays(ODD_STATE)[r],
                                    nonceLists[firstByte].statesBitarray[ODD_STATE],
                                )
                        val evenCount =
                            HardnestedSumProperty
                                .partSumA0Bitarrays(EVEN_STATE)[q]
                                .fourWayAndPopcount(
                                    HardnestedSumProperty.partSumA8Bitarrays(EVEN_STATE)[s],
                                    nonceLists[firstByte].statesBitarray[EVEN_STATE],
                                    nonceLists[firstByte xor 0x80].statesBitarray[EVEN_STATE],
                                )
                        numStates += oddCount.toLong() * evenCount.toLong()
                    }
                }
            }
        }
        return numStates
    }

    // ---- all_bitflips_match filter (port of lines 1759-1934) ----

    private fun evenparity8(x: Int): Int = (x and 0xFF).countOneBits() and 1

    /**
     * Port of invariant_holds() line 1759.
     */
    private fun invariantHolds(
        byteDiff: Int,
        state1: Int,
        state2: Int,
        bit: Int,
        stateBit: Int,
    ): Boolean {
        val j1BitMask = 1 shl (bit - 1)
        val bitDiff = byteDiff and j1BitMask
        val filterDiff =
            Crypto1.filter((state1 ushr (4 - stateBit)).toUInt()) xor
                Crypto1.filter((state2 ushr (4 - stateBit)).toUInt())
        val maskY12Y13 = 0xC0 ushr stateBit
        val stateBitsDiff = (state1 xor state2) and maskY12Y13
        val allDiff = evenparity8(bitDiff xor stateBitsDiff xor filterDiff)
        return allDiff == 0
    }

    /**
     * Port of invalid_state() line 1769.
     */
    private fun invalidState(
        byteDiff: Int,
        state1: Int,
        state2: Int,
        bit: Int,
        stateBit: Int,
    ): Boolean {
        val jBitMask = 1 shl bit
        val bitDiff = byteDiff and jBitMask
        val maskY13Y16 = 0x48 ushr stateBit
        val stateBitsDiff = (state1 xor state2) and maskY13Y16
        val allDiff = evenparity8(bitDiff xor stateBitsDiff)
        return allDiff != 0
    }

    /**
     * Port of remaining_bits_match() line 1778.
     * Uses fall-through switch logic from C: case N falls through to case N+1.
     */
    private fun remainingBitsMatch(
        numCommon: Int,
        byteDiff: Int,
        state1: Int,
        state2: Int,
        oddEven: Int,
    ): Boolean {
        if (oddEven == ODD_STATE) {
            if (numCommon <= 0 && !invariantHolds(byteDiff, state1, state2, 1, 0)) return true
            if (numCommon <= 1 && invalidState(byteDiff, state1, state2, 1, 0)) return false
            if (numCommon <= 2 && !invariantHolds(byteDiff, state1, state2, 3, 1)) return true
            if (numCommon <= 3 && invalidState(byteDiff, state1, state2, 3, 1)) return false
            if (numCommon <= 4 && !invariantHolds(byteDiff, state1, state2, 5, 2)) return true
            if (numCommon <= 5 && invalidState(byteDiff, state1, state2, 5, 2)) return false
            if (numCommon <= 6 && !invariantHolds(byteDiff, state1, state2, 7, 3)) return true
            if (numCommon <= 7 && invalidState(byteDiff, state1, state2, 7, 3)) return false
        } else {
            if (numCommon <= 0 && invalidState(byteDiff, state1, state2, 0, 0)) return false
            if (numCommon <= 1 && !invariantHolds(byteDiff, state1, state2, 2, 1)) return true
            if (numCommon <= 2 && invalidState(byteDiff, state1, state2, 2, 1)) return false
            if (numCommon <= 3 && !invariantHolds(byteDiff, state1, state2, 4, 2)) return true
            if (numCommon <= 4 && invalidState(byteDiff, state1, state2, 4, 2)) return false
            if (numCommon <= 5 && !invariantHolds(byteDiff, state1, state2, 6, 3)) return true
            if (numCommon <= 6 && invalidState(byteDiff, state1, state2, 6, 3)) return false
        }
        return true
    }

    /**
     * Bit-reverse a byte. Port of reverse() line 1885.
     */
    private fun reverseByte(b: Int): Int {
        var v = b and 0xFF
        v = ((v and 0x55) shl 1) or ((v ushr 1) and 0x55)
        v = ((v and 0x33) shl 2) or ((v ushr 2) and 0x33)
        v = ((v and 0x0F) shl 4) or ((v ushr 4) and 0x0F)
        return v
    }

    /**
     * Port of bitflips_match() line 1865.
     * Checks if state is in nonces[byte].states_bitarray[oddEven].
     */
    private fun bitflipsMatch(
        byte: Int,
        state: Int,
        oddEven: Int,
    ): Boolean = nonceLists[byte].statesBitarray[oddEven].test(state)

    /**
     * Per-state consistency check across all observed first-byte bitflip pairs.
     * Port of all_bitflips_match() line 1889.
     *
     * For each of 255 possible bitflip values, checks if the candidate state
     * is consistent with the observed parity relationship between the best
     * first byte and the corresponding flipped byte.
     */
    private fun allBitflipsMatch(
        byte: Int,
        state: Int,
        oddEven: Int,
    ): Boolean {
        val masks =
            if (oddEven == EVEN_STATE) {
                intArrayOf(
                    0x00FFFFF0,
                    0x00FFFFF8.toInt(),
                    0x00FFFFF8.toInt(),
                    0x00FFFFFC.toInt(),
                    0x00FFFFFC.toInt(),
                    0x00FFFFFE.toInt(),
                    0x00FFFFFE.toInt(),
                    0x00FFFFFF,
                )
            } else {
                intArrayOf(
                    0x00FFFFF0,
                    0x00FFFFF0,
                    0x00FFFFF8.toInt(),
                    0x00FFFFF8.toInt(),
                    0x00FFFFFC.toInt(),
                    0x00FFFFFC.toInt(),
                    0x00FFFFFE.toInt(),
                    0x00FFFFFE.toInt(),
                )
            }

        for (i in 1 until 256) {
            val bytesDiff = reverseByte(i)
            val byte2 = byte xor bytesDiff
            val numCommon = (bytesDiff and 0xFF).countTrailingZeroBits().coerceAtMost(7)
            val mask = masks[numCommon]
            var foundMatch = false
            val maxRemainingBits = mask.inv() and 0xFF
            for (remainingBits in 0..maxRemainingBits) {
                val state2 = (state and mask) or remainingBits
                if (remainingBitsMatch(numCommon, bytesDiff, state, state2, oddEven)) {
                    if (bitflipsMatch(byte2, state2, oddEven)) {
                        foundMatch = true
                        break
                    }
                }
            }
            if (!foundMatch) return false
        }
        return true
    }

    // ---- Candidate generation (port of generate_candidates) ----

    private data class StateList(
        val oddStates: IntArray,
        val evenStates: IntArray,
    )

    /**
     * Generate candidate state lists for a given (sum_a0_idx, sum_a8_idx) pair.
     * Port of generate_candidates() / generate_candidates_worker_thread() / add_matching_states().
     *
     * For each valid (p, q, r, s) tuple:
     *   - ODD candidates = part_sum_a8[ODD][p] AND part_sum_a0[ODD][r] AND nonces[best].states[ODD]
     *   - EVEN candidates = part_sum_a8[EVEN][q] AND part_sum_a0[EVEN][s] AND nonces[best].states[EVEN]
     *
     * Bitarray mapping (matches Proxmark3's add_matching_states exactly):
     * p,q (from sum_a0/byte 0) → part_sum_a0 (top 20 bits [23:4]);
     * r,s (from sum_a8/byte 1) → part_sum_a8 (bottom 20 bits [19:0]).
     *
     * Returns list of StateList(oddStates, evenStates) tuples.
     */
    private fun generateCandidates(
        sumA0Idx: Int,
        sumA8Idx: Int,
    ): List<StateList> {
        val sumA0 = SUMS[sumA0Idx]
        val sumA8 = SUMS[sumA8Idx]
        val bestByte = bestFirstBytes[0]
        val result = mutableListOf<StateList>()

        for (p in 0 until NUM_PART_SUMS) {
            for (q in 0 until NUM_PART_SUMS) {
                if (2 * p * (16 - 2 * q) + (16 - 2 * p) * 2 * q != sumA0) continue
                for (r in 0 until NUM_PART_SUMS) {
                    for (s in 0 until NUM_PART_SUMS) {
                        if (2 * r * (16 - 2 * s) + (16 - 2 * r) * 2 * s != sumA8) continue

                        // 3-way AND: part_sum_a0[p] AND part_sum_a8[r] AND nonces[best].states_bitarray
                        val oddBitarray = HardnestedSumProperty.partSumA0Bitarrays(ODD_STATE)[p].copy()
                        oddBitarray.and(HardnestedSumProperty.partSumA8Bitarrays(ODD_STATE)[r])
                        oddBitarray.and(nonceLists[bestByte].statesBitarray[ODD_STATE])

                        val oddCount = oddBitarray.popcount()
                        if (oddCount == 0) continue

                        val evenBitarray = HardnestedSumProperty.partSumA0Bitarrays(EVEN_STATE)[q].copy()
                        evenBitarray.and(HardnestedSumProperty.partSumA8Bitarrays(EVEN_STATE)[s])
                        evenBitarray.and(nonceLists[bestByte].statesBitarray[EVEN_STATE])

                        val evenCount = evenBitarray.popcount()
                        if (evenCount == 0) continue

                        // Extract state lists with all_bitflips_match filter
                        // Port of bitarray_to_list() which calls all_bitflips_match() per state
                        val oddList = mutableListOf<Int>()
                        oddBitarray.forEachSet { state ->
                            if (allBitflipsMatch(bestByte, state, ODD_STATE)) {
                                oddList.add(state)
                            }
                        }
                        if (oddList.isEmpty()) continue

                        val evenList = mutableListOf<Int>()
                        evenBitarray.forEachSet { state ->
                            if (allBitflipsMatch(bestByte, state, EVEN_STATE)) {
                                evenList.add(state)
                            }
                        }
                        if (evenList.isEmpty()) continue

                        val oddStates = oddList.toIntArray()
                        val evenStates = evenList.toIntArray()

                        result.add(StateList(oddStates, evenStates))
                    }
                }
            }
        }
        return result
    }

    // ---- Bitflip table initialization ----

    /** Precomputed bitflip bitarrays [oddEven][bitflip]. */
    private val bitflipBitarrays = Array(2) { arrayOfNulls<StateBitarray>(0x400) }
    private val countBitflipBitarrays = Array(2) { IntArray(0x400) { 1 shl 24 } }
    private val effectiveBitflip = Array(2) { IntArray(0x400) }
    private val numEffectiveBitflips = intArrayOf(0, 0)
    private var allEffectiveBitflip = IntArray(0x400)
    private var numAllEffectiveBitflips = 0
    private var num1stByteEffectiveBitflips = 0

    /**
     * Load precomputed bitflip bitarrays for both even-half and odd-half states.
     * Port of init_bitflip_bitarrays() lines 277-558.
     */
    private fun initBitflipBitarrays(): Boolean {
        if (!HardnestedBitflip.initialize()) return false

        // Load tables for both halves
        for (oddEven in 0..1) {
            for (bitflip in 0x001 until 0x400) {
                val table = HardnestedBitflip.loadBitflipTable(bitflip, oddEven)
                if (table != null) {
                    bitflipBitarrays[oddEven][bitflip] = table
                    countBitflipBitarrays[oddEven][bitflip] = table.popcount()
                    effectiveBitflip[oddEven][numEffectiveBitflips[oddEven]++] = bitflip
                }
            }
        }

        // Merge effective bitflip lists from both halves
        var i = 0
        var j = 0
        numAllEffectiveBitflips = 0
        num1stByteEffectiveBitflips = 0
        while (i < numEffectiveBitflips[EVEN_STATE] || j < numEffectiveBitflips[ODD_STATE]) {
            val evenVal = if (i < numEffectiveBitflips[EVEN_STATE]) effectiveBitflip[EVEN_STATE][i] else 0x400
            val oddVal = if (j < numEffectiveBitflips[ODD_STATE]) effectiveBitflip[ODD_STATE][j] else 0x400
            when {
                evenVal < oddVal -> {
                    allEffectiveBitflip[numAllEffectiveBitflips++] = evenVal
                    i++
                }
                evenVal > oddVal -> {
                    allEffectiveBitflip[numAllEffectiveBitflips++] = oddVal
                    j++
                }
                else -> {
                    allEffectiveBitflip[numAllEffectiveBitflips++] = evenVal
                    i++
                    j++
                }
            }
            if ((allEffectiveBitflip[numAllEffectiveBitflips - 1] and BITFLIP_2ND_BYTE) == 0) {
                num1stByteEffectiveBitflips = numAllEffectiveBitflips
            }
        }

        // Sort 1st byte bitflips by product of odd*even counts (most constraining first)
        val sorted1st =
            allEffectiveBitflip
                .take(num1stByteEffectiveBitflips)
                .sortedBy {
                    countBitflipBitarrays[ODD_STATE][it].toLong() * countBitflipBitarrays[EVEN_STATE][it].toLong()
                }
        sorted1st.forEachIndexed { idx, v -> allEffectiveBitflip[idx] = v }

        return true
    }

    // ---- Bitflip-only candidate generation (port of add_bitflip_candidates) ----

    /** Byte with smallest product of odd*even state counts. */
    private var bestFirstByteSmallestBitarray = 0

    // ---- Adaptive termination state (port of Proxmark3 reduction rate tracking) ----

    /** Sliding window of brute force estimates for linear regression. */
    private val reductionQueue = FloatArray(QUEUE_LEN) { (1L shl 48).toFloat() }

    /** Measured brute force throughput (state pairs per second). */
    private var bruteForcePerSecond: Float = DEFAULT_BRUTE_FORCE_RATE

    /** Time between consecutive shrink_key_space checks (ms), refined downward. */
    private var samplePeriodMs: Long = 2000

    /** Mark for measuring sample period. */
    private var lastSampleMark: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()

    /**
     * Find the first byte with smallest bitflip bitarray product.
     * Port of check_smallest_bitflip_bitarrays() lines 1033-1051.
     */
    private fun checkSmallestBitflipBitarrays(): Float {
        var smallest = 1L shl 48
        for (i in 0 until 256) {
            val numOdd = nonceLists[i].numStatesBitarray[ODD_STATE].toLong()
            val numEven = nonceLists[i].numStatesBitarray[EVEN_STATE].toLong()
            if (numOdd * numEven < smallest) {
                smallest = numOdd * numEven
                bestFirstByteSmallestBitarray = i
            }
        }
        return smallest.toFloat() / 2f
    }

    /**
     * Generate candidates using only bitflip-filtered states (no sum property).
     * Port of add_bitflip_candidates() lines 2016-2035.
     */
    private fun addBitflipCandidates(byte: Int): List<StateList> {
        val oddList = mutableListOf<Int>()
        nonceLists[byte].statesBitarray[ODD_STATE].forEachSet { state ->
            if (allBitflipsMatch(byte, state, ODD_STATE)) oddList.add(state)
        }
        val evenList = mutableListOf<Int>()
        nonceLists[byte].statesBitarray[EVEN_STATE].forEachSet { state ->
            if (allBitflipsMatch(byte, state, EVEN_STATE)) evenList.add(state)
        }
        return if (oddList.isNotEmpty() && evenList.isNotEmpty()) {
            listOf(StateList(oddList.toIntArray(), evenList.toIntArray()))
        } else {
            emptyList()
        }
    }

    // ---- Adaptive termination (port of update_reduction_rate + brute_force_benchmark) ----

    /**
     * Port of update_reduction_rate() from cmdhfmfhard.c lines 1161-1200.
     * Uses linear regression on a sliding window of the last [QUEUE_LEN] brute force estimates
     * to compute how fast the key space is shrinking per sample period.
     *
     * Returns the negative slope: positive means key space is shrinking, negative means growing.
     */
    private fun updateReductionRate(
        lastBruteForce: Float,
        init: Boolean,
    ): Float {
        if (init) {
            reductionQueue.fill((1L shl 48).toFloat())
            return 0f
        }

        // Shift queue left, append new value
        for (i in 0 until QUEUE_LEN - 1) {
            reductionQueue[i] = reductionQueue[i + 1]
        }
        reductionQueue[QUEUE_LEN - 1] = lastBruteForce

        // Linear regression: compute negative slope
        var avgX = 0f
        var avgY = 0f
        for (i in 0 until QUEUE_LEN) {
            avgX += i
            avgY += reductionQueue[i]
        }
        avgX /= QUEUE_LEN
        avgY /= QUEUE_LEN

        var devXY = 0f
        var devX2 = 0f
        for (i in 0 until QUEUE_LEN) {
            devXY += (i - avgX) * (reductionQueue[i] - avgY)
            devX2 += (i - avgX) * (i - avgX)
        }

        return -devXY / devX2 // negative slope = reduction rate
    }

    /**
     * Benchmark the brute force inner loop to measure throughput on this device.
     *
     * Port of brute_force_benchmark() from hardnested_bruteforce.c lines 485-523.
     * Instead of reading benchmark data files, we generate synthetic states and
     * time the parity-check inner loop.
     */
    private suspend fun bruteForceBenchmark(onProgress: ((String) -> Unit)?): Float {
        val benchSize = BENCH_SIZE
        val oddStates = IntArray(benchSize) { it * 3 } // synthetic odd states
        val evenStates = IntArray(benchSize) { it * 5 } // synthetic even states

        val mark = TimeSource.Monotonic.markNow()

        // Run the parity-check inner loop (same code path as real brute force)
        var tested = 0L
        val state = Crypto1State()
        for (oi in oddStates.indices) {
            val oddState = oddStates[oi].toUInt()
            for (evenState in evenStates) {
                state.odd = oddState
                state.even = evenState.toUInt()
                state.lfsrRollbackByte(0, true)
                // Just the parity check loop body cost
                for (byteIdx in 0 until 4) {
                    state.lfsrByte(0, true)
                    Crypto1.filter(state.odd)
                }
                tested++
            }
        }

        val elapsedMs = mark.elapsedNow().inWholeMilliseconds
        if (elapsedMs <= 0) {
            onProgress?.invoke(
                "  Brute force benchmark: too fast to measure, using default ${(DEFAULT_BRUTE_FORCE_RATE / 1_000_000).toLong()}M/s",
            )
            return DEFAULT_BRUTE_FORCE_RATE
        }

        val rate = tested.toFloat() / (elapsedMs / 1000f)
        onProgress?.invoke(
            "  Brute force benchmark: ${(rate / 1_000_000).toLong()}M (2^${formatFloat(log2(rate))}) keys/s",
        )
        return rate
    }

    /**
     * Serialize collected nonces to a byte array for persistence.
     *
     * Format:
     *   [4 bytes] uid (UInt, big-endian)
     *   [1 byte]  target block number
     *   [1 byte]  target key type
     *   [5 bytes × N] nonces:
     *     [4 bytes] nt_enc (UInt, big-endian)
     *     [1 byte]  par_enc (4-bit parity in low nibble)
     */
    fun serializeNonces(
        targetBlock: Int,
        targetKeyType: Byte,
    ): ByteArray {
        var totalEntries = 0
        for (i in 0 until 256) {
            totalEntries += nonceLists[i].entries.size
        }

        val buf = ByteArray(6 + 5 * totalEntries)
        // Header: uid (4 bytes BE) + targetBlock (1 byte) + targetKeyType (1 byte)
        buf[0] = (uid shr 24).toByte()
        buf[1] = (uid shr 16).toByte()
        buf[2] = (uid shr 8).toByte()
        buf[3] = uid.toByte()
        buf[4] = targetBlock.toByte()
        buf[5] = targetKeyType

        var offset = 6
        for (i in 0 until 256) {
            for (entry in nonceLists[i].entries) {
                buf[offset++] = (entry.nonceEnc shr 24).toByte()
                buf[offset++] = (entry.nonceEnc shr 16).toByte()
                buf[offset++] = (entry.nonceEnc shr 8).toByte()
                buf[offset++] = entry.nonceEnc.toByte()
                buf[offset++] = (entry.parEnc and 0x0F).toByte()
            }
        }
        return buf
    }

    /**
     * Deserialize nonces from a byte array and replay them via [addNonce].
     * Returns the number of unique nonces loaded, or null if the header doesn't match.
     */
    fun deserializeNonces(
        data: ByteArray,
        expectedUid: UInt,
        expectedBlock: Int,
        expectedKeyType: Byte,
    ): Int? {
        if (data.size < 6) return null

        val fileUid =
            ((data[0].toInt() and 0xFF).toUInt() shl 24) or
                ((data[1].toInt() and 0xFF).toUInt() shl 16) or
                ((data[2].toInt() and 0xFF).toUInt() shl 8) or
                (data[3].toInt() and 0xFF).toUInt()
        val fileBlock = data[4].toInt() and 0xFF
        val fileKeyType = data[5]

        if (fileUid != expectedUid || fileBlock != expectedBlock || fileKeyType != expectedKeyType) {
            return null
        }

        var numLoaded = 0
        var offset = 6
        while (offset + 5 <= data.size) {
            val ntEnc =
                ((data[offset].toInt() and 0xFF).toUInt() shl 24) or
                    ((data[offset + 1].toInt() and 0xFF).toUInt() shl 16) or
                    ((data[offset + 2].toInt() and 0xFF).toUInt() shl 8) or
                    (data[offset + 3].toInt() and 0xFF).toUInt()
            val parEnc = data[offset + 4].toInt() and 0x0F
            numLoaded += addNonce(ntEnc, parEnc)
            offset += 5
        }
        return numLoaded
    }

    // ---- Main attack flow ----

    suspend fun recoverKey(
        knownKeyType: Byte,
        knownSectorBlock: Int,
        knownKey: Long,
        targetKeyType: Byte,
        targetBlock: Int,
        onProgress: ((String) -> Unit)? = null,
        onNoncesCollected: ((ByteArray) -> Unit)? = null,
    ): Long? {
        // Initialize bitflip tables
        onProgress?.invoke("Loading bitflip tables...")
        val tablesAvailable = initBitflipBitarrays()
        onProgress?.invoke(
            "  Loaded ${numEffectiveBitflips[EVEN_STATE]} even + ${numEffectiveBitflips[ODD_STATE]} odd bitflip tables",
        )

        // Initialize sum property prior
        val pK = FloatArray(NUM_SUMS)
        val priors = HardnestedSumProperty.sumProbabilities()
        for (i in 0 until NUM_SUMS) pK[i] = priors[i].toFloat()

        // ---- Parity self-test: verify parity extraction using known key ----
        onProgress?.invoke("Parity self-test...")
        val parityTestOk = selfTestParity(knownKeyType, knownSectorBlock, knownKey, onProgress)
        if (!parityTestOk) {
            onProgress?.invoke("FATAL: Parity self-test failed — encrypted parity extraction is broken")
            return null
        }
        onProgress?.invoke("  Parity self-test PASSED")

        // ---- Benchmark brute force throughput (port of line 2432) ----
        onProgress?.invoke("Running brute force benchmark...")
        bruteForcePerSecond = bruteForceBenchmark(onProgress)

        // Initialize reduction rate tracker (port of line 2571)
        updateReductionRate(0f, init = true)
        samplePeriodMs = 2000
        lastSampleMark = TimeSource.Monotonic.markNow()

        // ---- Phase 1: Collect encrypted nonces ----
        onProgress?.invoke("Phase 1: Collecting nonces...")

        var numAcquiredNonces = 0
        var sumA0Applied = false
        var consecutiveFailures = 0

        for (round in 0 until COLLECTION_ROUNDS) {
            val reselected = rawClassic.hardReselectCard()
            if (!reselected) {
                consecutiveFailures++
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    throw CardLostException("Card lost during nonce collection")
                }
                continue
            }

            val authState = rawClassic.authenticate(knownKeyType, knownSectorBlock, knownKey)
            if (authState == null) {
                consecutiveFailures++
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    throw CardLostException("Cannot authenticate with known key")
                }
                continue
            }
            consecutiveFailures = 0

            val result =
                rawClassic.nestedAuth(targetKeyType, targetBlock, authState)
                    ?: continue

            numAcquiredNonces += addNonce(result.encryptedNonce, result.encryptedParity)

            if ((round + 1) % 500 == 0) {
                onProgress?.invoke("  $numAcquiredNonces unique nonces ($firstByteNum/256 first bytes)")
            }

            // Once all 256 first bytes are seen, enter Phase 2
            if (firstByteNum == 256 && !sumA0Applied) {
                // Snap first_byte_Sum to index
                var gotMatch = false
                for (i in 0 until NUM_SUMS) {
                    if (firstByteSum == SUMS[i]) {
                        firstByteSum = i
                        gotMatch = true
                        break
                    }
                }
                if (!gotMatch) {
                    onProgress?.invoke("No match for first_byte_Sum ($firstByteSum). Not a genuine MFC EV1?")
                    return null
                }
                onProgress?.invoke("  Sum(a0) = ${SUMS[firstByteSum]} (index $firstByteSum)")
                applySumA0()
                sumA0Applied = true
            }

            // Update nonce data (bitflip filtering + sum estimation)
            if (sumA0Applied) {
                checkForBitFlipProperties()
                updateAllBitflipsArray()
                updateSumBitarrays(EVEN_STATE)
                updateSumBitarrays(ODD_STATE)
                if (pKDirty) {
                    updatePK(pK)
                    pKDirty = false
                }
                estimateSumA8(pK)
            } else if (tablesAvailable) {
                // Before sum_a0 is applied, only check bitflip properties.
                // update_allbitflips_array is gated on CHECK_2ND_BYTES in Proxmark3 (line 899).
                checkForBitFlipProperties()
            }

            // Check termination: port of shrink_key_space() lines 1203-1220
            if (numAcquiredNonces >= MIN_NONCES && (round + 1) % SHRINK_CHECK_INTERVAL == 0) {
                val bf1 = checkSmallestBitflipBitarrays()
                val bf2 = if (sumA0Applied) sortBestFirstBytes(pK) else (1L shl 47).toFloat()
                val bf = minOf(bf1, bf2)

                val reductionRate = updateReductionRate(bf, init = false)
                val bruteForcePerSample = bruteForcePerSecond * samplePeriodMs / 1000f

                // Update sample period: refine downward (port of lines 1744-1745)
                val elapsed = lastSampleMark.elapsedNow().inWholeMilliseconds
                if (elapsed < samplePeriodMs) samplePeriodMs = elapsed
                lastSampleMark = TimeSource.Monotonic.markNow()

                onProgress?.invoke(
                    "  [$numAcquiredNonces nonces] bf=${bf.toLong()}, " +
                        "reduction=${reductionRate.toLong()}/sample, " +
                        "bf/sample=${bruteForcePerSample.toLong()}",
                )

                // Port of shrink_key_space() return condition (lines 1216-1218):
                // Stop collecting when the key space is shrinking slower than brute force
                // could test, OR when the absolute threshold is reached.
                val acquisitionComplete =
                    sumA0Applied &&
                        reductionRate >= 0f &&
                        (reductionRate < bruteForcePerSample || bf < BRUTE_FORCE_THRESHOLD)

                if (acquisitionComplete) {
                    onProgress?.invoke("  Acquisition complete — proceeding to brute force (bf=${bf.toLong()})")
                    break
                }
            }
        }

        if (!sumA0Applied) {
            onProgress?.invoke("Failed: did not observe all 256 first bytes ($firstByteNum/256)")
            return null
        }

        // ---- Save nonces for offline restart (port of nonce_file_write) ----
        if (onNoncesCollected != null) {
            val nonceData = serializeNonces(targetBlock, targetKeyType)
            onNoncesCollected.invoke(nonceData)
            onProgress?.invoke("  Saved ${(nonceData.size - 6) / 5} nonces for offline restart")
        }

        // ---- Phase 2-3: Generate candidates and brute force ----
        // Port of lines 2619-2672 in cmdhfmfhard.c

        // Collect all nonces for verification
        val allNonces = mutableListOf<NonceData>()
        for (i in 0 until 256) {
            for (entry in nonceLists[i].entries) {
                allNonces.add(NonceData(entry.nonceEnc, entry.parEnc))
            }
        }

        // Compare bitflip-only vs sum-property approach
        val expectedBruteForce1 = checkSmallestBitflipBitarrays()
        val expectedBruteForce2 = nonceLists[bestFirstBytes[0]].expectedNumBruteForce

        if (expectedBruteForce1 < expectedBruteForce2) {
            // Bitflip-only is cheaper — ignore sum property
            onProgress?.invoke("Ignoring Sum(a8) properties — bitflip-only is cheaper")
            onProgress?.invoke("  Best bitflip byte: 0x${bestFirstByteSmallestBitarray.toString(16).padStart(2, '0')}")
            onProgress?.invoke("  Expected brute force: ${expectedBruteForce1.toLong()}")

            val candidates = addBitflipCandidates(bestFirstByteSmallestBitarray)
            var totalStates = 0L
            for (sl in candidates) {
                totalStates += sl.oddStates.size.toLong() * sl.evenStates.size.toLong()
            }
            onProgress?.invoke("  $totalStates candidate pairs")

            for (sl in candidates) {
                val key =
                    bruteForceStateList(
                        sl.oddStates,
                        sl.evenStates,
                        bestFirstByteSmallestBitarray,
                        allNonces,
                        targetKeyType,
                        targetBlock,
                        onProgress,
                    )
                if (key != null) {
                    onProgress?.invoke("Key recovered: 0x${key.toString(16).padStart(12, '0')}")
                    return key
                }
            }
        } else {
            // Sum-property approach
            val bestByte = bestFirstBytes[0]
            onProgress?.invoke("Best first byte: 0x${bestByte.toString(16).padStart(2, '0')}")
            onProgress?.invoke("  Expected brute force: ${expectedBruteForce2.toLong()}")

            // Iterate sum_a8 guesses for the best first byte
            // Port of lines 2650-2672 in cmdhfmfhard.c
            for (j in 0 until NUM_SUMS) {
                val guess = nonceLists[bestByte].sumA8Guess[j]
                if (guess.prob <= 0f) continue

                onProgress?.invoke(
                    "  ${j + 1}. guess: Sum(a8) = ${SUMS[guess.sumA8Idx]} (prob=${formatFloat(guess.prob)})",
                )

                val candidates = generateCandidates(firstByteSum, guess.sumA8Idx)

                var totalStates = 0L
                for (sl in candidates) {
                    totalStates += sl.oddStates.size.toLong() * sl.evenStates.size.toLong()
                }
                onProgress?.invoke("    ${candidates.size} tuples, $totalStates total candidate pairs")

                // Update num_states for this guess
                guess.numStates = totalStates
                updateExpectedBruteForce(bestByte)

                if (totalStates == 0L) continue

                // Brute force each tuple
                for ((tupleIdx, sl) in candidates.withIndex()) {
                    val pairs = sl.oddStates.size.toLong() * sl.evenStates.size.toLong()
                    onProgress?.invoke(
                        "    Tuple ${tupleIdx + 1}/${candidates.size}: ${sl.oddStates.size} x ${sl.evenStates.size} = $pairs",
                    )

                    val key =
                        bruteForceStateList(
                            sl.oddStates,
                            sl.evenStates,
                            bestFirstBytes[0],
                            allNonces,
                            targetKeyType,
                            targetBlock,
                            onProgress,
                        )
                    if (key != null) {
                        onProgress?.invoke("Key recovered: 0x${key.toString(16).padStart(12, '0')}")
                        return key
                    }
                }

                // Failed with this sum_a8 guess — zero it out and try next
                guess.prob = 0f
                guess.numStates = 0
                updateExpectedBruteForce(bestByte)
            }
        }

        onProgress?.invoke("Hardnested attack failed: exhausted all sum_a8 guesses")
        return null
    }

    /**
     * Update expected brute force for a first byte after changing probabilities.
     * Port of update_expected_brute_force() lines 1053-1071.
     */
    private fun updateExpectedBruteForce(bestByte: Int) {
        val nl = nonceLists[bestByte]
        // Normalize probabilities
        var totalProb = 0f
        for (i in 0 until NUM_SUMS) totalProb += nl.sumA8Guess[i].prob
        if (totalProb > 0) {
            for (i in 0 until NUM_SUMS) nl.sumA8Guess[i].prob /= totalProb
        }
        var probAllFailed = 1f
        nl.expectedNumBruteForce = 0f
        for (i in 0 until NUM_SUMS) {
            nl.expectedNumBruteForce += nl.sumA8Guess[i].prob * nl.sumA8Guess[i].numStates.toFloat() / 2f
            probAllFailed -= nl.sumA8Guess[i].prob
            nl.expectedNumBruteForce += probAllFailed * nl.sumA8Guess[i].numStates.toFloat() / 2f
        }
    }

    // ---- Key verification ----

    private fun verifyKeyWithNonce(
        key: Long,
        encNonce: UInt,
        encParity: Int,
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

    // ---- Brute force ----

    private suspend fun bruteForceStateList(
        oddStates: IntArray,
        evenStates: IntArray,
        bestFirstByte: Int,
        nonces: List<NonceData>,
        targetKeyType: Byte,
        targetBlock: Int,
        onProgress: ((String) -> Unit)?,
    ): Long? {
        val primaryNonce = nonces[0]

        val encBytes =
            IntArray(4) { byteIdx ->
                ((primaryNonce.encryptedNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            }
        val encParBits =
            IntArray(4) { byteIdx ->
                (primaryNonce.encryptedParity shr (3 - byteIdx)) and 1
            }
        val uidXorEnc = uid xor primaryNonce.encryptedNonce
        val inputBytes =
            IntArray(4) { byteIdx ->
                ((uidXorEnc shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            }

        // Rollback input: uid_byte0 XOR encrypted_nonce_first_byte
        // Candidates are post-byte-0 states (matching Proxmark3 convention).
        // We roll back 1 encrypted byte to recover the initial key state.
        val rollbackInput = ((uid shr 24).toInt() and 0xFF) xor bestFirstByte

        val verifyNonces =
            nonces
                .drop(1)
                .filterIndexed { idx, _ -> idx % (nonces.size / VERIFY_NONCE_COUNT).coerceAtLeast(1) == 0 }
                .take(VERIFY_NONCE_COUNT)

        val chunkSize = maxOf(1, oddStates.size / NUM_PARALLEL_CHUNKS)
        val chunks =
            (0 until oddStates.size step chunkSize).map { start ->
                start until minOf(start + chunkSize, oddStates.size)
            }

        var testedTotal = 0L
        val mark = TimeSource.Monotonic.markNow()

        val result =
            coroutineScope {
                val deferreds =
                    chunks.map { range ->
                        async(Dispatchers.Default) {
                            var tested = 0L
                            val candidates = mutableListOf<Long>()
                            val state = Crypto1State()
                            val verifyState = Crypto1State()

                            for (oi in range) {
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
                                    // and check parity (same as original).
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

                                    var allNoncesPass = true
                                    for (vn in verifyNonces) {
                                        if (!verifyKeyWithNonce(
                                                candidateKey,
                                                vn.encryptedNonce,
                                                vn.encryptedParity,
                                                verifyState,
                                            )
                                        ) {
                                            allNoncesPass = false
                                            break
                                        }
                                    }
                                    if (!allNoncesPass) continue
                                    candidates.add(candidateKey)
                                }
                            }
                            tested to candidates
                        }
                    }

                val allCandidates = mutableListOf<Long>()
                for ((chunkIdx, deferred) in deferreds.withIndex()) {
                    val (tested, candidates) = deferred.await()
                    testedTotal += tested
                    allCandidates.addAll(candidates)

                    val elapsed = mark.elapsedNow().inWholeMilliseconds / 1000.0
                    val rate = if (elapsed > 0) (testedTotal / elapsed / 1_000_000).toLong() else 0
                    onProgress?.invoke(
                        "      [${chunkIdx + 1}/${chunks.size}] $testedTotal pairs (${rate}M/s), ${allCandidates.size} candidates",
                    )
                }

                val uniqueKeys = allCandidates.toSet()
                if (uniqueKeys.isEmpty()) return@coroutineScope null

                if (uniqueKeys.size > MAX_CARD_VERIFY) {
                    onProgress?.invoke("      Too many candidates (${uniqueKeys.size}) — skipping card verify")
                    return@coroutineScope null
                }

                onProgress?.invoke("      Verifying ${uniqueKeys.size} keys with card...")
                for (candidateKey in uniqueKeys) {
                    val reselected = rawClassic.hardReselectCard()
                    if (!reselected) continue
                    val authResult = rawClassic.authenticate(targetKeyType, targetBlock, candidateKey)
                    rawClassic.restoreNormalMode()
                    if (authResult != null) return@coroutineScope candidateKey
                }
                null
            }

        return result
    }

    /**
     * Self-test: verify parity extraction by doing a nested auth to the KNOWN sector.
     *
     * Since we know the target key, we can initialize a fresh cipher and decrypt
     * the nested auth response nonce, then verify that the extracted parity bits
     * match the keystream parity bits.
     *
     * Protocol: after receiving the encrypted AUTH CMD, the card starts a NEW cipher
     * with the target key. It sends nT encrypted with:
     *   crypto1_init(state, target_key)
     *   ks0 = crypto1_word(state, uid ^ nT_enc, isEncrypted=true)
     *   nT = ks0 ^ nT_enc
     * The parity keystream bits for bytes 0-2 are at ks0 bits 16, 8, 0 (BEBIT layout),
     * and for byte 3 it's filter(state.odd) after the word.
     *
     * This catches bugs in unpackWithParity or parity bit ordering.
     */
    private suspend fun selfTestParity(
        knownKeyType: Byte,
        knownSectorBlock: Int,
        knownKey: Long,
        onProgress: ((String) -> Unit)?,
    ): Boolean {
        repeat(5) { attempt ->
            val reselected = rawClassic.hardReselectCard()
            if (!reselected) return@repeat

            // Authenticate to known sector
            val authState =
                rawClassic.authenticate(knownKeyType, knownSectorBlock, knownKey)
                    ?: return@repeat

            // Nested auth to the SAME sector (same key, so we can verify)
            val result =
                rawClassic.nestedAuth(knownKeyType, knownSectorBlock, authState)
                    ?: return@repeat

            val encNonce = result.encryptedNonce
            val encParity = result.encryptedParity

            // Initialize fresh cipher with target key (same as known key for self-test)
            val state = Crypto1State()
            state.loadKey(knownKey)

            // Feed encrypted nonce XOR uid (Proxmark3: crypto1_word(pcs, nt_enc ^ uid, 1))
            val ks0 = state.lfsrWord(encNonce xor uid, true)
            val plainNonce = ks0 xor encNonce

            // Parity keystream bits (from Proxmark3 protocol demo):
            //   byte 0: (ks0 >> 16) & 1
            //   byte 1: (ks0 >> 8) & 1
            //   byte 2: (ks0 >> 0) & 1
            //   byte 3: filter(state.odd)  (33rd bit, after the 32-bit word)
            val ksParBits =
                intArrayOf(
                    (ks0.toInt() shr 16) and 1,
                    (ks0.toInt() shr 8) and 1,
                    ks0.toInt() and 1,
                    Crypto1.filter(state.odd),
                )

            var allMatch = true
            for (byteIdx in 0 until 4) {
                val plainByte = ((plainNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
                val encByte = ((encNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
                val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksParBits[byteIdx]
                val actualEncPar = (encParity shr (3 - byteIdx)) and 1

                if (expectedEncPar != actualEncPar) {
                    onProgress?.invoke(
                        "  Parity mismatch at byte $byteIdx: expected=$expectedEncPar actual=$actualEncPar",
                    )
                    onProgress?.invoke(
                        "    encByte=0x${encByte.toString(
                            16,
                        ).padStart(
                            2,
                            '0',
                        )} plainByte=0x${plainByte.toString(16).padStart(2, '0')} ksPar=${ksParBits[byteIdx]}",
                    )
                    allMatch = false
                }
            }

            if (allMatch) {
                onProgress?.invoke("  Self-test: all 4 parity bits verified on attempt ${attempt + 1}")
                return true
            } else {
                onProgress?.invoke("  Self-test: FAILED on attempt ${attempt + 1}")
                return false // Don't retry — systematic error
            }
        }

        onProgress?.invoke("  Self-test: could not complete (5 attempts failed to auth)")
        return false
    }

    companion object {
        private fun formatFloat(value: Float): String {
            val intPart = value.toLong()
            val fracPart = ((value - intPart) * 1000 + 0.5).toLong()
            return "$intPart.${fracPart.toString().padStart(3, '0')}"
        }

        const val EVEN_STATE = 0
        const val ODD_STATE = 1
        const val NUM_SUMS = 19
        const val NUM_PART_SUMS = 9
        const val BITFLIP_2ND_BYTE = 0x0200

        val SUMS = intArrayOf(0, 32, 56, 64, 80, 96, 104, 112, 120, 128, 136, 144, 152, 160, 176, 192, 200, 224, 256)

        const val MIN_NONCES = 500
        const val COLLECTION_ROUNDS = 20000
        const val MAX_CONSECUTIVE_FAILURES = 5
        const val MAX_CARD_VERIFY = 5_000
        const val VERIFY_NONCE_COUNT = 8
        const val NUM_PARALLEL_CHUNKS = 8
        const val SHRINK_CHECK_INTERVAL = 100
        const val BRUTE_FORCE_THRESHOLD = 0xF00000.toFloat() // Proxmark3's fixed termination threshold
        const val QUEUE_LEN = 4 // sliding window size for reduction rate regression
        const val DEFAULT_BRUTE_FORCE_RATE = 34_000_000f // fallback if benchmark fails (based on hardware tests)
        const val BENCH_SIZE = 6000 // number of odd/even states for benchmark (matches Proxmark3 TEST_BENCH_SIZE)
    }
}
