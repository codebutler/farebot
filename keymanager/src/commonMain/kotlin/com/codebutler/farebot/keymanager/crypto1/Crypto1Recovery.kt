/*
 * Crypto1Recovery.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * MIFARE Classic Crypto1 key recovery algorithms.
 * Faithful port of crapto1 by bla <blapost@gmail.com>.
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
 * MIFARE Classic Crypto1 key recovery algorithms.
 *
 * Implements LFSR state recovery from known keystream, based on the
 * approach from crapto1 by bla. Given 32 bits of known keystream
 * (extracted during authentication), this recovers candidate
 * 48-bit LFSR states that could have produced that keystream.
 *
 * The recovered states can then be rolled back through the authentication
 * initialization to extract the 48-bit sector key.
 *
 * Reference: crapto1 lfsr_recovery32() from Proxmark3 / mfoc / mfcuk
 */
@OptIn(ExperimentalUnsignedTypes::class)
object Crypto1Recovery {
    /**
     * Recover candidate LFSR states from 32 bits of known keystream.
     *
     * Port of crapto1's lfsr_recovery32(). The algorithm:
     * 1. Split keystream into odd-indexed and even-indexed bits (BEBIT order)
     * 2. Build tables of filter-consistent 20-bit values for each half
     * 3. Extend tables to 24 bits using 4 more keystream bits each
     * 4. Recursively extend and merge using feedback relation
     * 5. Return all matching (odd, even) state pairs
     *
     * @param ks2 32 bits of known keystream
     * @param input The value that was fed into the LFSR during keystream generation.
     *              Use 0 if keystream was generated with no input (e.g., mfkey32 attack).
     *              Use uid XOR nT if keystream was generated during cipher init
     *              (e.g., nested attack on the encrypted nonce).
     * @return List of candidate [Crypto1State] objects.
     */
    fun lfsrRecovery32(
        ks2: UInt,
        input: UInt,
    ): List<Crypto1State> {
        // Split keystream into odd-indexed and even-indexed bits.
        var oks = 0u
        var eks = 0u
        var i = 31
        while (i >= 0) {
            oks = oks shl 1 or Crypto1.bebit(ks2, i)
            i -= 2
        }
        i = 30
        while (i >= 0) {
            eks = eks shl 1 or Crypto1.bebit(ks2, i)
            i -= 2
        }

        // Allocate arrays large enough for in-place extend_table_simple.
        val arraySize = 1 shl 22
        val oddTbl = UIntArray(arraySize)
        val evenTbl = UIntArray(arraySize)
        var oddEnd = -1
        var evenEnd = -1

        // Fill initial tables: all values in [0, 2^20] whose filter
        // output matches the first keystream bit for each half.
        for (v in (1 shl 20) downTo 0) {
            if (Crypto1.filter(v.toUInt()).toUInt() == (oks and 1u)) {
                oddTbl[++oddEnd] = v.toUInt()
            }
            if (Crypto1.filter(v.toUInt()).toUInt() == (eks and 1u)) {
                evenTbl[++evenEnd] = v.toUInt()
            }
        }

        // Extend tables from 20 bits to 24 bits (4 rounds of extend_table_simple).
        for (round in 0 until 4) {
            oks = oks shr 1
            oddEnd = extendTableSimpleInPlace(oddTbl, oddEnd, (oks and 1u).toInt())
            eks = eks shr 1
            evenEnd = extendTableSimpleInPlace(evenTbl, evenEnd, (eks and 1u).toInt())
        }

        // Copy to right-sized arrays for recovery phase
        val oddArr = oddTbl.copyOfRange(0, oddEnd + 1)
        val evenArr = evenTbl.copyOfRange(0, evenEnd + 1)

        // Transform the input parameter for recover(), matching C code:
        // in = (in >> 16 & 0xff) | (in << 16) | (in & 0xff00)
        val transformedInput =
            ((input shr 16) and 0xFFu) or
                (input shl 16) or
                (input and 0xFF00u)

        // Recover matching state pairs.
        val results = mutableListOf<Crypto1State>()
        recover(
            oddArr,
            oddArr.size,
            oks,
            evenArr,
            evenArr.size,
            eks,
            11,
            results,
            transformedInput shl 1,
        )

        return results
    }

    /**
     * In-place extend_table_simple, faithfully matching crapto1's pointer logic.
     *
     * @return New end index (inclusive)
     */
    private fun extendTableSimpleInPlace(
        tbl: UIntArray,
        endIdx: Int,
        bit: Int,
    ): Int {
        var end = endIdx
        var idx = 0

        while (idx <= end) {
            tbl[idx] = tbl[idx] shl 1
            val f0 = Crypto1.filter(tbl[idx])
            val f1 = Crypto1.filter(tbl[idx] or 1u)

            if (f0 != f1) {
                // Uniquely determined: set LSB = filter(v) ^ bit
                tbl[idx] = tbl[idx] or ((f0 xor bit).toUInt())
                idx++
            } else if (f0 == bit) {
                // Both match: keep both variants
                end++
                tbl[end] = tbl[idx + 1]
                tbl[idx + 1] = tbl[idx] or 1u
                idx += 2
            } else {
                // Neither matches: drop (replace with last entry)
                tbl[idx] = tbl[end]
                end--
            }
        }
        return end
    }

    /**
     * Extend a table of candidate values by one bit with contribution tracking.
     * Creates a NEW output array.
     *
     * Port of crapto1's extend_table().
     */
    private fun extendTable(
        data: UIntArray,
        size: Int,
        bit: UInt,
        m1: UInt,
        m2: UInt,
        inputBit: UInt,
    ): Pair<UIntArray, Int> {
        val inShifted = inputBit shl 24
        val output = UIntArray(size * 2 + 1)
        var outIdx = 0

        for (idx in 0 until size) {
            val shifted = data[idx] shl 1

            val f0 = Crypto1.filter(shifted).toUInt()
            val f1 = Crypto1.filter(shifted or 1u).toUInt()

            if (f0 != f1) {
                output[outIdx] = shifted or (f0 xor bit)
                updateContribution(output, outIdx, m1, m2)
                output[outIdx] = output[outIdx] xor inShifted
                outIdx++
            } else if (f0 == bit) {
                output[outIdx] = shifted
                updateContribution(output, outIdx, m1, m2)
                output[outIdx] = output[outIdx] xor inShifted
                outIdx++

                output[outIdx] = shifted or 1u
                updateContribution(output, outIdx, m1, m2)
                output[outIdx] = output[outIdx] xor inShifted
                outIdx++
            }
            // else: discard
        }

        return Pair(output, outIdx)
    }

    /**
     * Update the contribution bits (upper 8 bits) of a table entry.
     * Faithfully ported from crapto1's update_contribution().
     */
    private fun updateContribution(
        data: UIntArray,
        idx: Int,
        m1: UInt,
        m2: UInt,
    ) {
        val item = data[idx]
        var p = item shr 25
        p = p shl 1 or Crypto1.parity(item and m1)
        p = p shl 1 or Crypto1.parity(item and m2)
        data[idx] = p shl 24 or (item and 0xFFFFFFu)
    }

    /**
     * Recursively extend odd and even tables, then bucket-sort intersect
     * to find matching pairs.
     *
     * Port of Proxmark3's recover() using bucket sort for intersection.
     */
    private fun recover(
        oddData: UIntArray,
        oddSize: Int,
        oks: UInt,
        evenData: UIntArray,
        evenSize: Int,
        eks: UInt,
        rem: Int,
        results: MutableList<Crypto1State>,
        input: UInt,
    ) {
        if (oddSize == 0 || evenSize == 0) return

        if (rem == -1) {
            // Base case: assemble state pairs.
            for (eIdx in 0 until evenSize) {
                val eVal = evenData[eIdx]
                val eModified =
                    (eVal shl 1) xor
                        Crypto1.parity(eVal and Crypto1.LF_POLY_EVEN) xor
                        (if (input and 4u != 0u) 1u else 0u)
                for (oIdx in 0 until oddSize) {
                    val oVal = oddData[oIdx]
                    results.add(
                        Crypto1State(
                            even = oVal,
                            odd = eModified xor Crypto1.parity(oVal and Crypto1.LF_POLY_ODD),
                        ),
                    )
                }
            }
            return
        }

        // Extend both tables by up to 4 more keystream bits
        var curOddData = oddData
        var curOddSize = oddSize
        var curEvenData = evenData
        var curEvenSize = evenSize
        var oksLocal = oks
        var eksLocal = eks
        var inputLocal = input
        var remLocal = rem

        for (round in 0 until 4) {
            // C: for(i = 0; i < 4 && rem--; i++)
            if (remLocal == 0) {
                remLocal = -1
                break
            }
            remLocal--

            oksLocal = oksLocal shr 1
            eksLocal = eksLocal shr 1
            inputLocal = inputLocal shr 2

            val oddResult =
                extendTable(
                    curOddData,
                    curOddSize,
                    oksLocal and 1u,
                    Crypto1.LF_POLY_EVEN shl 1 or 1u,
                    Crypto1.LF_POLY_ODD shl 1,
                    0u,
                )
            curOddData = oddResult.first
            curOddSize = oddResult.second
            if (curOddSize == 0) return

            val evenResult =
                extendTable(
                    curEvenData,
                    curEvenSize,
                    eksLocal and 1u,
                    Crypto1.LF_POLY_ODD,
                    Crypto1.LF_POLY_EVEN shl 1 or 1u,
                    inputLocal and 3u,
                )
            curEvenData = evenResult.first
            curEvenSize = evenResult.second
            if (curEvenSize == 0) return
        }

        // Bucket sort intersection on upper 8 bits (contribution bits).
        val oddBuckets = HashMap<Int, MutableList<Int>>()
        for (idx in 0 until curOddSize) {
            val bucket = (curOddData[idx] shr 24).toInt()
            oddBuckets.getOrPut(bucket) { mutableListOf() }.add(idx)
        }

        val evenBuckets = HashMap<Int, MutableList<Int>>()
        for (idx in 0 until curEvenSize) {
            val bucket = (curEvenData[idx] shr 24).toInt()
            evenBuckets.getOrPut(bucket) { mutableListOf() }.add(idx)
        }

        for ((bucket, oddIndices) in oddBuckets) {
            val evenIndices = evenBuckets[bucket] ?: continue

            val oddSub = UIntArray(oddIndices.size) { curOddData[oddIndices[it]] }
            val evenSub = UIntArray(evenIndices.size) { curEvenData[evenIndices[it]] }

            recover(
                oddSub,
                oddSub.size,
                oksLocal,
                evenSub,
                evenSub.size,
                eksLocal,
                remLocal,
                results,
                inputLocal,
            )
        }
    }

    /**
     * Calculate the distance (number of PRNG steps) between two nonces.
     *
     * @param n1 Starting nonce
     * @param n2 Target nonce
     * @return Number of PRNG steps from [n1] to [n2], or [UInt.MAX_VALUE]
     *         if [n2] is not reachable from [n1] within 65536 steps.
     */
    fun nonceDistance(
        n1: UInt,
        n2: UInt,
    ): UInt {
        var state = n1
        for (i in 0u until 65536u) {
            if (state == n2) return i
            state = Crypto1.prngSuccessor(state, 1u)
        }
        return UInt.MAX_VALUE
    }

    /**
     * High-level key recovery from nested authentication data.
     *
     * @param uid Card UID (4 bytes)
     * @param knownNT Card nonce from the known-key authentication
     * @param encryptedNT Encrypted nonce from the nested authentication
     * @param knownKey The known sector key (48 bits)
     * @return List of candidate 48-bit keys for the target sector
     */
    fun recoverKeyFromNonces(
        uid: UInt,
        knownNT: UInt,
        encryptedNT: UInt,
        knownKey: Long,
    ): List<Long> {
        val recoveredKeys = mutableListOf<Long>()

        val state = Crypto1Auth.initCipher(knownKey, uid, knownNT)
        state.lfsrWord(0u, false)
        state.lfsrWord(0u, false)
        val ks = state.lfsrWord(0u, false)
        val candidateNT = encryptedNT xor ks

        val candidates = lfsrRecovery32(ks, candidateNT)
        for (candidate in candidates) {
            val s = candidate.copy()
            s.lfsrRollbackWord(uid xor candidateNT, false)
            recoveredKeys.add(s.getKey())
        }

        return recoveredKeys
    }
}
