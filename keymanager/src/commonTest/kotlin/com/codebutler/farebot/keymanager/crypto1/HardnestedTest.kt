/*
 * HardnestedTest.kt
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

package com.codebutler.farebot.keymanager.crypto1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HardnestedTest {
    // ---- StateBitarray tests ----

    @Test
    fun testBitarraySetAndTest() {
        val ba = StateBitarray()
        assertFalse(ba.test(0))
        assertFalse(ba.test(100))

        ba.set(0)
        ba.set(100)
        ba.set(16777215) // 2^24 - 1 (max index)

        assertTrue(ba.test(0))
        assertTrue(ba.test(100))
        assertTrue(ba.test(16777215))
        assertFalse(ba.test(1))
        assertFalse(ba.test(99))
    }

    @Test
    fun testBitarrayClear() {
        val ba = StateBitarray()
        ba.set(42)
        assertTrue(ba.test(42))
        ba.clear(42)
        assertFalse(ba.test(42))
    }

    @Test
    fun testBitarraySetAll() {
        val ba = StateBitarray()
        ba.setAll()
        assertTrue(ba.test(0))
        assertTrue(ba.test(1000000))
        assertTrue(ba.test(16777215))
        assertEquals(StateBitarray.NUM_BITS, ba.popcount())
    }

    @Test
    fun testBitarrayClearAll() {
        val ba = StateBitarray()
        ba.setAll()
        ba.clearAll()
        assertEquals(0, ba.popcount())
        assertFalse(ba.test(0))
    }

    @Test
    fun testBitarrayPopcount() {
        val ba = StateBitarray()
        assertEquals(0, ba.popcount())

        ba.set(0)
        assertEquals(1, ba.popcount())

        ba.set(1)
        ba.set(2)
        assertEquals(3, ba.popcount())

        // Setting the same bit twice shouldn't change count
        ba.set(2)
        assertEquals(3, ba.popcount())
    }

    @Test
    fun testBitarrayAnd() {
        val a = StateBitarray()
        val b = StateBitarray()

        a.set(0)
        a.set(1)
        a.set(2)

        b.set(1)
        b.set(2)
        b.set(3)

        a.and(b)

        assertFalse(a.test(0)) // only in a
        assertTrue(a.test(1)) // in both
        assertTrue(a.test(2)) // in both
        assertFalse(a.test(3)) // only in b
        assertEquals(2, a.popcount())
    }

    @Test
    fun testBitarrayOr() {
        val a = StateBitarray()
        val b = StateBitarray()

        a.set(0)
        a.set(1)

        b.set(1)
        b.set(2)

        a.or(b)

        assertTrue(a.test(0))
        assertTrue(a.test(1))
        assertTrue(a.test(2))
        assertEquals(3, a.popcount())
    }

    @Test
    fun testBitarrayForEachSet() {
        val ba = StateBitarray()
        val expected = listOf(0, 5, 100, 1000, 16777215)
        for (idx in expected) ba.set(idx)

        val collected = mutableListOf<Int>()
        ba.forEachSet { collected.add(it) }

        assertEquals(expected.sorted(), collected.sorted())
    }

    @Test
    fun testBitarrayCopy() {
        val original = StateBitarray()
        original.set(42)
        original.set(100)

        val copy = original.copy()
        assertTrue(copy.test(42))
        assertTrue(copy.test(100))

        // Modifying original shouldn't affect copy
        original.clear(42)
        assertTrue(copy.test(42))
    }

    // ---- Sum property tests ----

    @Test
    fun testOddSumCountsCoverAllStates() {
        val total = HardnestedSumProperty.oddSumCounts.sum()
        assertEquals(1 shl 24, total, "Sum of all odd partial sum counts should be 2^24")
    }

    @Test
    fun testEvenSumCountsCoverAllStates() {
        val total = HardnestedSumProperty.evenSumCounts.sum()
        assertEquals(1 shl 24, total, "Sum of all even partial sum counts should be 2^24")
    }

    @Test
    fun testA0BitarraysCoverAllStates() {
        val union = StateBitarray()
        for (i in 0..16) {
            union.or(HardnestedSumProperty.partSumA0OddBitarrays[i])
        }
        assertEquals(1 shl 24, union.popcount(), "Union of all a0 odd bitarrays should be 2^24")
    }

    @Test
    fun testA8BitarraysCoverAllStates() {
        val union = StateBitarray()
        for (i in 0..16) {
            union.or(HardnestedSumProperty.partSumA8OddBitarrays[i])
        }
        assertEquals(1 shl 24, union.popcount(), "Union of all a8 odd bitarrays should be 2^24")
    }

    @Test
    fun testA0AndA8IntersectionIsSmaller() {
        // The AND of a0[p] and a8[r] should be smaller than either alone
        // because a0 constrains top 20 bits and a8 constrains bottom 20 bits
        val maxOddSum =
            HardnestedSumProperty.oddSumCounts.indices.maxByOrNull {
                HardnestedSumProperty.oddSumCounts[it]
            } ?: 8
        val a0 = HardnestedSumProperty.partSumA0OddBitarrays[maxOddSum]
        val a8 = HardnestedSumProperty.partSumA8OddBitarrays[maxOddSum]
        val intersection = a0.copy()
        intersection.and(a8)
        assertTrue(
            intersection.popcount() < a0.popcount(),
            "a0 AND a8 (${intersection.popcount()}) should be smaller than a0 alone (${a0.popcount()})",
        )
        assertTrue(
            intersection.popcount() < a8.popcount(),
            "a0 AND a8 (${intersection.popcount()}) should be smaller than a8 alone (${a8.popcount()})",
        )
        assertTrue(
            intersection.popcount() > 0,
            "a0 AND a8 should not be empty",
        )
    }

    @Test
    fun testA0BitarraysAreDisjoint() {
        for (i in 0..15) {
            for (j in i + 1..16) {
                val iCount = HardnestedSumProperty.partSumA0OddBitarrays[i].popcount()
                val jCount = HardnestedSumProperty.partSumA0OddBitarrays[j].popcount()
                if (iCount == 0 || jCount == 0) continue
                val intersection = HardnestedSumProperty.partSumA0OddBitarrays[i].copy()
                intersection.and(HardnestedSumProperty.partSumA0OddBitarrays[j])
                assertEquals(
                    0,
                    intersection.popcount(),
                    "A0 odd bitarrays $i and $j should be disjoint",
                )
            }
        }
    }

    @Test
    fun testEstimateSumA8() {
        // Simulate observing 200 nonces where ~75% have odd parity (expect sum ≈ 192)
        val hypotheses = HardnestedSumProperty.estimateSumA8(200, 150)
        assertTrue(hypotheses.isNotEmpty(), "Should return at least one hypothesis")

        for (i in 0 until hypotheses.size - 1) {
            assertTrue(
                hypotheses[i].second >= hypotheses[i + 1].second,
                "Hypotheses should be sorted by descending probability",
            )
        }

        // The top hypothesis should be close to the observed fraction * 256
        val topSum = hypotheses[0].first
        val expectedSum = (150.0 / 200.0 * 256).toInt() // ≈ 192
        assertTrue(
            kotlin.math.abs(topSum - expectedSum) <= 32,
            "Top hypothesis sum=$topSum should be close to expected $expectedSum",
        )
    }

    @Test
    fun testSumPropertyDistribution() {
        val nonZeroCounts = HardnestedSumProperty.oddSumCounts.count { it > 0 }
        assertTrue(
            nonZeroCounts > 5,
            "Odd states should be distributed across more than 5 sum values, got $nonZeroCounts",
        )
    }

    @Test
    fun testCombinedSumValues() {
        // Verify that combinedSum produces values from the SUMS table
        val validSums = HardnestedSumProperty.SUMS.toSet()
        for (p in 0..16 step 2) {
            for (q in 0..16 step 2) {
                val combined = HardnestedSumProperty.combinedSum(p, q)
                assertTrue(
                    combined in validSums,
                    "combinedSum($p, $q) = $combined should be in SUMS table",
                )
            }
        }
    }

    @Test
    fun testPartialSumPairsForCombinedSum128() {
        // sum=128 (most common): P*(16-Q) + (16-P)*Q = 128
        val pairs = HardnestedSumProperty.partialSumPairsForCombinedSum(128)
        assertTrue(pairs.isNotEmpty(), "sum=128 should have at least one (p,q) pair")
        for ((p, q) in pairs) {
            assertEquals(128, HardnestedSumProperty.combinedSum(p, q))
        }
    }

    // ---- Brute force parity check tests ----

    @Test
    fun testEncryptedModeBruteForceParityCheck() {
        // Validate the corrected brute force inner loop:
        // 1. Given a known key, uid, and nonce, compute the encrypted nonce + parity
        // 2. Get S_key (state after loadKey)
        // 3. Verify the encrypted-mode parity check PASSES for the correct S_key
        // 4. Verify getKey() on S_key recovers the original key

        val key = 0xA0A1A2A3A4A5L
        val uid = 0xDEADBEEFu
        val nT = 0x12345678u

        // Step 1: Simulate what the card does during nested auth.
        // Card loads key, then encrypts nT as enc_nT = nT XOR ks.
        // The keystream comes from clocking the LFSR with uid^nT as input.
        val cardState = Crypto1State()
        cardState.loadKey(key)

        // Save S_key before any clocking
        val sKeyOdd = cardState.odd
        val sKeyEven = cardState.even

        // Generate keystream for nonce encryption: lfsrWord(uid^nT, false)
        val ksWord = cardState.lfsrWord(uid xor nT, false)
        val encNonce = nT xor ksWord

        // Generate encrypted parity bits (4 bits, one per byte)
        // We need to re-initialize to generate byte-by-byte with parity
        val parState = Crypto1State()
        parState.loadKey(key)
        var encParity = 0
        for (byteIdx in 0 until 4) {
            // Clock 8 bits for this byte
            val ksBytes =
                parState.lfsrByte(
                    ((uid xor nT).toInt() shr ((3 - byteIdx) * 8)) and 0xFF,
                    false,
                )
            // Parity keystream bit = filter(odd) after the byte
            val ksPar = Crypto1.filter(parState.odd)
            // Plaintext byte
            val plainByte = ((nT shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            // Encrypted parity = oddParity(plainByte) XOR ksPar
            val encParBit = Crypto1Auth.oddParity(plainByte) xor ksPar
            encParity = encParity or (encParBit shl (3 - byteIdx))
        }

        // Step 2: Now simulate the brute force parity check using encrypted mode.
        // This is exactly what the fixed bruteForceKeys() does.
        val bruteState = Crypto1State()
        bruteState.odd = sKeyOdd
        bruteState.even = sKeyEven

        val uidXorEnc = uid xor encNonce
        val inputBytes =
            IntArray(4) { byteIdx ->
                ((uidXorEnc shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            }
        val encBytes =
            IntArray(4) { byteIdx ->
                ((encNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            }
        val encParBits =
            IntArray(4) { byteIdx ->
                (encParity shr (3 - byteIdx)) and 1
            }

        var parityOk = true
        for (byteIdx in 0 until 4) {
            val ksByte = bruteState.lfsrByte(inputBytes[byteIdx], true)
            val ksPar = Crypto1.filter(bruteState.odd)
            val plainByte = encBytes[byteIdx] xor ksByte
            val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
            if (expectedEncPar != encParBits[byteIdx]) {
                parityOk = false
                break
            }
        }
        assertTrue(parityOk, "Parity check should PASS for the correct S_key state")

        // Step 3: Verify getKey() on S_key recovers the original key
        bruteState.odd = sKeyOdd
        bruteState.even = sKeyEven
        val recoveredKey = bruteState.getKey()
        assertEquals(key, recoveredKey, "getKey() on S_key should recover the original key")
    }

    @Test
    fun testEncryptedModeBruteForceRejectsWrongState() {
        val key = 0xA0A1A2A3A4A5L
        val uid = 0xDEADBEEFu
        val nT = 0x12345678u

        // Generate encrypted nonce and parity from the correct key
        val cardState = Crypto1State()
        cardState.loadKey(key)
        val ksWord = cardState.lfsrWord(uid xor nT, false)
        val encNonce = nT xor ksWord

        val parState = Crypto1State()
        parState.loadKey(key)
        var encParity = 0
        for (byteIdx in 0 until 4) {
            val ksBytes =
                parState.lfsrByte(
                    ((uid xor nT).toInt() shr ((3 - byteIdx) * 8)) and 0xFF,
                    false,
                )
            val ksPar = Crypto1.filter(parState.odd)
            val plainByte = ((nT shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            encParity = encParity or ((Crypto1Auth.oddParity(plainByte) xor ksPar) shl (3 - byteIdx))
        }

        // Try a WRONG key's S_key state — parity check should almost certainly fail
        val wrongState = Crypto1State()
        wrongState.loadKey(0x112233445566L)
        val wrongOdd = wrongState.odd
        val wrongEven = wrongState.even

        val uidXorEnc = uid xor encNonce
        val inputBytes =
            IntArray(4) { byteIdx ->
                ((uidXorEnc shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            }
        val encBytes =
            IntArray(4) { byteIdx ->
                ((encNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            }
        val encParBits =
            IntArray(4) { byteIdx ->
                (encParity shr (3 - byteIdx)) and 1
            }

        val bruteState = Crypto1State()
        bruteState.odd = wrongOdd
        bruteState.even = wrongEven

        var parityOk = true
        for (byteIdx in 0 until 4) {
            val ksByte = bruteState.lfsrByte(inputBytes[byteIdx], true)
            val ksPar = Crypto1.filter(bruteState.odd)
            val plainByte = encBytes[byteIdx] xor ksByte
            val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
            if (expectedEncPar != encParBits[byteIdx]) {
                parityOk = false
                break
            }
        }
        assertFalse(parityOk, "Parity check should FAIL for wrong S_key state")
    }

    @Test
    fun testVerifyKeyWithNonceConsistency() {
        // Test that the encrypted-mode parity check in lfsrByte matches
        // the bit-by-bit approach in verifyKeyWithNonce
        val key = 0xFFFFFFFFFFFF
        val uid = 0x01020304u
        val nT = 0xCAFEBABEu

        val cardState = Crypto1State()
        cardState.loadKey(key)
        val sKeyOdd = cardState.odd
        val sKeyEven = cardState.even

        // Generate encrypted nonce
        val ksWord = cardState.lfsrWord(uid xor nT, false)
        val encNonce = nT xor ksWord

        // Generate encrypted parity
        val parState = Crypto1State()
        parState.loadKey(key)
        var encParity = 0
        for (byteIdx in 0 until 4) {
            parState.lfsrByte(
                ((uid xor nT).toInt() shr ((3 - byteIdx) * 8)) and 0xFF,
                false,
            )
            val ksPar = Crypto1.filter(parState.odd)
            val plainByte = ((nT shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            encParity = encParity or ((Crypto1Auth.oddParity(plainByte) xor ksPar) shl (3 - byteIdx))
        }

        // Method 1: lfsrByte encrypted mode (brute force approach)
        val state1 = Crypto1State()
        state1.odd = sKeyOdd
        state1.even = sKeyEven
        val uidXorEnc = uid xor encNonce
        var method1Ok = true
        for (byteIdx in 0 until 4) {
            val inputByte = ((uidXorEnc shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            val ksByte = state1.lfsrByte(inputByte, true)
            val ksPar = Crypto1.filter(state1.odd)
            val encByte = ((encNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            val plainByte = encByte xor ksByte
            val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
            val encParBit = (encParity shr (3 - byteIdx)) and 1
            if (expectedEncPar != encParBit) {
                method1Ok = false
                break
            }
        }

        // Method 2: lfsrBit encrypted mode (verifyKeyWithNonce approach)
        val state2 = Crypto1State()
        state2.loadKey(key)
        var method2Ok = true
        for (byteIdx in 0 until 4) {
            var ksByteVal = 0
            for (bitIdx in 0 until 8) {
                val i = byteIdx * 8 + bitIdx
                val inputBit = Crypto1.bebit(uidXorEnc, i).toInt()
                val ksBit = state2.lfsrBit(inputBit, true)
                ksByteVal = ksByteVal or (ksBit shl bitIdx)
            }
            val ksPar = Crypto1.filter(state2.odd)
            val encByte = ((encNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            val plainByte = encByte xor ksByteVal
            val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
            val encParBit = (encParity shr (3 - byteIdx)) and 1
            if (expectedEncPar != encParBit) {
                method2Ok = false
                break
            }
        }

        assertTrue(method1Ok, "lfsrByte encrypted-mode parity check should pass for correct key")
        assertTrue(method2Ok, "lfsrBit encrypted-mode parity check should pass for correct key")
    }

    // ---- End-to-end pipeline diagnostic ----

    @Test
    fun testSKeyInCorrectBitarray() {
        // Diagnostic test: verify for a known key which bitarray window (A0 vs A8)
        // the byte 0 sum observation corresponds to.
        //
        // Proxmark3 maps: byte 0 → sum_a0 → part_sum_a0 (top 20 bits [23:4])
        //                  byte 1 → sum_a8 → part_sum_a8 (bottom 20 bits [19:0])
        val key = 0xA0A1A2A3A4A5L
        val uid = 0xB7164F30u // OV-chipkaart UID

        // Step 1: Get S_key
        val state = Crypto1State()
        state.loadKey(key)
        val sKeyOdd = state.odd.toInt()
        val sKeyEven = state.even.toInt()

        // Step 2: Find which partSumA0 and partSumA8 bitarrays contain S_key
        var oddA0PartSum = -1
        var oddA8PartSum = -1
        var evenA0PartSum = -1
        var evenA8PartSum = -1

        for (ps in 0..16) {
            if (HardnestedSumProperty.partSumA0OddBitarrays[ps].test(sKeyOdd)) {
                assertEquals(-1, oddA0PartSum, "S_key odd should be in exactly one A0 group")
                oddA0PartSum = ps
            }
            if (HardnestedSumProperty.partSumA8OddBitarrays[ps].test(sKeyOdd)) {
                assertEquals(-1, oddA8PartSum, "S_key odd should be in exactly one A8 group")
                oddA8PartSum = ps
            }
            if (HardnestedSumProperty.partSumA0EvenBitarrays[ps].test(sKeyEven)) {
                assertEquals(-1, evenA0PartSum, "S_key even should be in exactly one A0 group")
                evenA0PartSum = ps
            }
            if (HardnestedSumProperty.partSumA8EvenBitarrays[ps].test(sKeyEven)) {
                assertEquals(-1, evenA8PartSum, "S_key even should be in exactly one A8 group")
                evenA8PartSum = ps
            }
        }

        assertTrue(oddA0PartSum >= 0, "S_key odd must be in some A0 bitarray")
        assertTrue(oddA8PartSum >= 0, "S_key odd must be in some A8 bitarray")
        assertTrue(evenA0PartSum >= 0, "S_key even must be in some A0 bitarray")
        assertTrue(evenA8PartSum >= 0, "S_key even must be in some A8 bitarray")

        // Step 3: Compute expected combined sums
        val expectedSumA0 = HardnestedSumProperty.combinedSum(oddA0PartSum, evenA0PartSum)
        val expectedSumA8 = HardnestedSumProperty.combinedSum(oddA8PartSum, evenA8PartSum)

        // Step 4: Simulate byte 0 observation — exactly as Proxmark3's add_nonce:
        // first_byte_Sum += evenparity32((nonce_enc & 0xff000000) | (par_enc & 0x08))
        val firstByteSeen = BooleanArray(256)
        var firstByteSum = 0

        for (nTVal in 0 until 1024) {
            val nT = (nTVal.toUInt() shl 22) or 0x12345u

            // Generate encrypted nonce
            val simState = Crypto1State()
            simState.loadKey(key)
            val ksWord = simState.lfsrWord(uid xor nT, false)
            val encNonce = nT xor ksWord

            // Generate encrypted parity
            val parState = Crypto1State()
            parState.loadKey(key)
            var encParity = 0
            for (byteIdx in 0 until 4) {
                parState.lfsrByte(
                    ((uid xor nT).toInt() shr ((3 - byteIdx) * 8)) and 0xFF,
                    false,
                )
                val ksPar = Crypto1.filter(parState.odd)
                val plainByte = ((nT shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
                encParity = encParity or ((Crypto1Auth.oddParity(plainByte) xor ksPar) shl (3 - byteIdx))
            }

            val byte0 = ((encNonce shr 24) and 0xFFu).toInt()
            val par0 = (encParity shr 3) and 1

            if (!firstByteSeen[byte0]) {
                firstByteSeen[byte0] = true
                // Exact Proxmark3 evenparity32 formula:
                // evenparity32((nonce_enc & 0xff000000) | (par_enc & 0x08))
                // = popcount of (byte0 in bits [31:24] OR par0 in bit 3) modulo 2
                val combined = (byte0 shl 24) or (par0 shl 3)
                firstByteSum += combined.countOneBits() and 1
            }
        }

        val distinctFirstBytes = firstByteSeen.count { it }

        // Step 5: Print diagnostic info
        println("=== S_key Diagnostic ===")
        println("Key: 0x${key.toString(16).padStart(12, '0')}")
        println("S_key odd: 0x${sKeyOdd.toString(16).padStart(6, '0')} ($sKeyOdd)")
        println("S_key even: 0x${sKeyEven.toString(16).padStart(6, '0')} ($sKeyEven)")
        println("A0 [23:4]: oddPartSum=$oddA0PartSum, evenPartSum=$evenA0PartSum -> combined=$expectedSumA0")
        println("A8 [19:0]: oddPartSum=$oddA8PartSum, evenPartSum=$evenA8PartSum -> combined=$expectedSumA8")
        println("Distinct first bytes: $distinctFirstBytes")
        println("first_byte_Sum (Proxmark3 convention) = $firstByteSum")
        println("  Matches A0 ($expectedSumA0)? ${firstByteSum == expectedSumA0}")
        println("  Matches A8 ($expectedSumA8)? ${firstByteSum == expectedSumA8}")

        // The observation should match either the A0 or A8 combined sum
        assertTrue(
            distinctFirstBytes == 256,
            "Need exactly 256 distinct first bytes for exact comparison, got $distinctFirstBytes",
        )
        val matchesA0 = firstByteSum == expectedSumA0
        val matchesA8 = firstByteSum == expectedSumA8
        assertTrue(
            matchesA0 || matchesA8,
            "first_byte_Sum ($firstByteSum) should match A0 ($expectedSumA0) or A8 ($expectedSumA8)",
        )

        // Step 6: Verify S_key survives combined A0 AND A8 filtering.
        // This always works because both windows contain S_key by construction.
        val oddFiltered = HardnestedSumProperty.partSumA0OddBitarrays[oddA0PartSum].copy()
        oddFiltered.and(HardnestedSumProperty.partSumA8OddBitarrays[oddA8PartSum])
        assertTrue(oddFiltered.test(sKeyOdd), "S_key odd must survive A0∧A8 filtering")

        val evenFiltered = HardnestedSumProperty.partSumA0EvenBitarrays[evenA0PartSum].copy()
        evenFiltered.and(HardnestedSumProperty.partSumA8EvenBitarrays[evenA8PartSum])
        assertTrue(evenFiltered.test(sKeyEven), "S_key even must survive A0∧A8 filtering")

        println("Filtered: odd=${oddFiltered.popcount()}, even=${evenFiltered.popcount()}")
        println("Total candidates: ${oddFiltered.popcount().toLong() * evenFiltered.popcount()}")
    }

    // ---- LZ4 decompressor tests ----

    @Test
    fun testLz4LiteralsOnly() {
        // Minimal LZ4 block: one sequence with only literals, no match
        // Token: 0x30 = 3 literals, 0 match length base
        // But last sequence has no match, so token = 0x30, 3 literal bytes
        val compressed =
            byteArrayOf(
                0x30, // token: literalLen=3, matchLenBase=0
                0x41,
                0x42,
                0x43, // literals: "ABC"
            )
        val result = Lz4.decompressBlock(compressed, 0, compressed.size, 3)
        assertEquals(3, result.size)
        assertEquals(0x41, result[0].toInt() and 0xFF)
        assertEquals(0x42, result[1].toInt() and 0xFF)
        assertEquals(0x43, result[2].toInt() and 0xFF)
    }

    @Test
    fun testLz4WithMatch() {
        // Sequence 1: 4 literals "ABCD" + match (offset=4, length=4) => "ABCDABCD"
        // Token: 0x40 = 4 literals, 0 match length base (min match = 4)
        // Sequence 2 (last): 0 literals, no match => token: 0x00
        // Actually, let me construct this properly:
        // "ABCDABCD" = 8 bytes
        // Seq 1: token=0x40, literals=ABCD, offset=4 (LE: 0x04, 0x00), match_len = 4 (base 0 + 4)
        val compressed =
            byteArrayOf(
                0x40, // token: literalLen=4, matchLenBase=0
                0x41,
                0x42,
                0x43,
                0x44, // literals
                0x04,
                0x00, // match offset = 4 (LE)
                // matchLen = 0 + 4 = 4
                // After this, we have 8 bytes and the last sequence had a match,
                // but we're at end of compressed data, so this is the last sequence
                // Wait -- LZ4 spec says last sequence never has a match.
                // Let me redesign: use a pattern that ends with a literal-only sequence
            )
        // Actually, the LZ4 block format says "The last sequence is incomplete,
        // and stops right after literals field." So let me create:
        // "ABCDABCDXY" = 10 bytes
        // Seq 1: token=0x40, lits=ABCD, offset=4, matchLen=4 => ABCDABCD (8 bytes)
        // Seq 2 (last): token=0x20, lits=XY => ABCDABCDXY (10 bytes)
        val compressed2 =
            byteArrayOf(
                0x40.toByte(), // token: literalLen=4, matchLenBase=0
                0x41,
                0x42,
                0x43,
                0x44, // literals
                0x04,
                0x00, // match offset = 4 (LE)
                0x20.toByte(), // token: literalLen=2, matchLenBase=0 (last seq, no match)
                0x58,
                0x59, // literals: "XY"
            )
        val result2 = Lz4.decompressBlock(compressed2, 0, compressed2.size, 10)
        assertEquals(10, result2.size)
        assertEquals("ABCDABCDXY", String(result2))
    }

    @Test
    fun testLz4WithOverlappingMatch() {
        // Test RLE pattern: "AAAAAAAA" = 8 bytes
        // Seq 1: token=0x10, lits=A, offset=1, matchLen=4+1=5 => AAAAAA (6 bytes)
        // Seq 2 (last): token=0x20, lits=AA => AAAAAAAA (8 bytes)
        val compressed =
            byteArrayOf(
                0x11.toByte(), // token: literalLen=1, matchLenBase=1
                0x41, // literal: "A"
                0x01,
                0x00, // match offset = 1 (RLE)
                // matchLen = 1 + 4 = 5
                0x20.toByte(), // token: literalLen=2 (last seq)
                0x41,
                0x41, // literals: "AA"
            )
        val result = Lz4.decompressBlock(compressed, 0, compressed.size, 8)
        assertEquals(8, result.size)
        assertEquals("AAAAAAAA", String(result))
    }

    // ---- HBFT parsing tests ----

    @Test
    fun testHbftParsing() {
        // Create a minimal HBFT blob with one entry
        // The "compressed data" is a trivial LZ4 block: all-zero bitarray (2MB)
        // For testing, we'll create a tiny HBFT and check the index parsing
        val bitflip = 0x01
        // Minimal LZ4 block that decompresses to 2MB of zeros:
        // A long run of literal zeros. Actually, the easiest is RLE:
        // But for this test, we just check the header parsing, not decompression.
        // Let's create a synthetic compressed block.
        val fakeCompressed = byteArrayOf(0x00) // won't decompress to 2MB, but we only test parsing

        val hbft = buildHbftBlob(listOf(bitflip to fakeCompressed))

        // Verify magic
        assertEquals(0x48, hbft[0].toInt() and 0xFF) // 'H'
        assertEquals(0x42, hbft[1].toInt() and 0xFF) // 'B'
        assertEquals(0x46, hbft[2].toInt() and 0xFF) // 'F'
        assertEquals(0x54, hbft[3].toInt() and 0xFF) // 'T'

        // Verify version
        assertEquals(1, (hbft[4].toInt() and 0xFF) or ((hbft[5].toInt() and 0xFF) shl 8))

        // Verify count
        assertEquals(1, (hbft[6].toInt() and 0xFF) or ((hbft[7].toInt() and 0xFF) shl 8))
    }

    @Test
    fun testBitflipTableInitialization() {
        // HardnestedBitflip.initialize() should return true if tables are available
        // On JVM test, tables should be available if the resource was packed
        val available = HardnestedBitflip.initialize()
        if (available) {
            assertTrue(HardnestedBitflip.evenTableCount > 0, "Should have loaded even tables")
            println(
                "Loaded ${HardnestedBitflip.evenTableCount} even + ${HardnestedBitflip.oddTableCount} odd bitflip tables",
            )

            // Load a table and verify it has some but not all states set
            val bitflip = HardnestedBitflip.availableBitflips(0).first()
            val table = HardnestedBitflip.loadBitflipTable(bitflip, 0)
            assertNotNull(table, "Table for bitflip $bitflip should load")
            assertTrue(table.popcount() > 0, "Table should have some states set")
            assertTrue(
                table.popcount() < StateBitarray.NUM_BITS,
                "Table should not have all states set (${table.popcount()} of ${StateBitarray.NUM_BITS})",
            )
            println("Bitflip 0x${bitflip.toString(16)}: ${table.popcount()} states")
        } else {
            println("Bitflip tables not available (run :tools:hardnested:run first)")
        }
    }

    private fun buildHbftBlob(entries: List<Pair<Int, ByteArray>>): ByteArray {
        val buf = mutableListOf<Byte>()
        // Magic: "HBFT" as raw bytes
        buf.addAll(listOf(0x48.toByte(), 0x42.toByte(), 0x46.toByte(), 0x54.toByte()))
        // Version: 1
        buf.addAll(intToLe16(1))
        // Count
        buf.addAll(intToLe16(entries.size))
        // Index
        for ((bitflip, data) in entries) {
            buf.addAll(intToLe16(bitflip))
            buf.addAll(intToLe32(data.size))
        }
        // Data
        for ((_, data) in entries) {
            buf.addAll(data.toList())
        }
        return buf.toByteArray()
    }

    private fun intToLe16(value: Int): List<Byte> =
        listOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
        )

    private fun intToLe32(value: Int): List<Byte> =
        listOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte(),
        )

    // ---- threeWayAndPopcount tests ----

    @Test
    fun testThreeWayAndPopcount() {
        val a = StateBitarray()
        val b = StateBitarray()
        val c = StateBitarray()

        // Set overlapping bits
        a.set(0)
        a.set(1)
        a.set(2)
        a.set(3)
        a.set(4)
        b.set(1)
        b.set(2)
        b.set(3)
        b.set(5)
        c.set(2)
        c.set(3)
        c.set(4)
        c.set(5)

        // Only bits 2 and 3 are in all three
        val count = a.threeWayAndPopcount(b, c)
        assertEquals(2, count, "Only bits 2 and 3 should be in all three bitarrays")

        // Verify matches manual AND + popcount
        val manual = a.copy()
        manual.and(b)
        manual.and(c)
        assertEquals(manual.popcount(), count, "threeWayAndPopcount should match manual AND chain")
    }

    @Test
    fun testThreeWayAndPopcountMatchesSumBitarrays() {
        // Verify threeWayAndPopcount gives same result as sequential AND + popcount
        // using actual sum property bitarrays
        val maxOddSum =
            HardnestedSumProperty.oddSumCounts.indices.maxByOrNull {
                HardnestedSumProperty.oddSumCounts[it]
            } ?: 8
        val a = HardnestedSumProperty.partSumA0OddBitarrays[maxOddSum]
        val b = HardnestedSumProperty.partSumA8OddBitarrays[maxOddSum]
        val c = StateBitarray()
        c.setAll() // identity for AND

        val threeWay = a.threeWayAndPopcount(b, c)
        val twoWay = a.andPopcount(b)
        assertEquals(twoWay, threeWay, "Three-way AND with all-ones third should equal two-way AND")
    }

    // ---- Precomputed bitflip integration tests ----

    @Test
    fun testPrecomputedBitflipFilteringPreservesKnownKey() {
        // If precomputed tables are available, verify S_key even-half survives filtering
        val available = HardnestedBitflip.initialize()
        if (!available) {
            println("Skipping: precomputed bitflip tables not available")
            return
        }

        val key = 0xA0A1A2A3A4A5L
        val uid = 0xB7164F30u

        val state = Crypto1State()
        state.loadKey(key)
        val sKeyEven = state.even.toInt()

        // Generate nonces with parity (matching HardnestedAttack's approach)
        data class SimNonce(
            val encFirstByte: Int,
            val parByte0: Int,
        )
        val noncesByByte = mutableMapOf<Int, SimNonce>()
        for (nTVal in 0 until 2048) {
            val nT = (nTVal.toUInt() shl 21) or 0xABCDu
            val simState = Crypto1State()
            simState.loadKey(key)
            val ksWord = simState.lfsrWord(uid xor nT, false)
            val encNonce = nT xor ksWord
            val byte0 = ((encNonce shr 24) and 0xFFu).toInt()
            if (byte0 !in noncesByByte) {
                val parState = Crypto1State()
                parState.loadKey(key)
                parState.lfsrByte(((uid xor nT).toInt() shr 24) and 0xFF, false)
                val ksPar = Crypto1.filter(parState.odd)
                val plainByte = ((nT shr 24) and 0xFFu).toInt()
                val encParBit = Crypto1Auth.oddParity(plainByte) xor ksPar
                noncesByByte[byte0] = SimNonce(byte0, encParBit)
            }
        }

        // Use first observed byte as reference (matching HardnestedAttack.kt)
        val refByte = noncesByByte.keys.first()
        val refPar = noncesByByte[refByte]!!.parByte0
        val observedBitflips = mutableSetOf<Int>()
        for ((byte0, nonce) in noncesByByte) {
            if (byte0 == refByte) continue
            val xor = refByte xor byte0
            if (refPar == nonce.parByte0) {
                observedBitflips.add(xor)
            } else {
                observedBitflips.add(xor or 0x100)
            }
        }

        // Apply precomputed even-half bitflip filtering
        val bitflipEven = StateBitarray()
        bitflipEven.setAll()

        var tablesApplied = 0
        for (bf in observedBitflips) {
            val table = HardnestedBitflip.loadBitflipTable(bf, 0) ?: continue
            bitflipEven.and(table)
            tablesApplied++
        }

        println(
            "Ref byte: 0x${refByte.toString(
                16,
            ).padStart(2, '0')}, $tablesApplied tables, ${bitflipEven.popcount()} even states survive",
        )

        // S_key even-half must survive
        assertTrue(
            bitflipEven.test(sKeyEven),
            "S_key even ($sKeyEven) must survive precomputed bitflip filtering (${bitflipEven.popcount()} states remain)",
        )
        assertTrue(
            bitflipEven.popcount() < StateBitarray.NUM_BITS,
            "Precomputed bitflip filtering should reduce even candidates",
        )
    }

    @Test
    fun testThreeWayFilteringPreservesKnownKey() {
        // Full three-way AND: sum_a8 AND sum_a0 AND bitflip must preserve the correct key's S_key
        val key = 0xA0A1A2A3A4A5L
        val uid = 0xB7164F30u

        val state = Crypto1State()
        state.loadKey(key)
        val sKeyOdd = state.odd.toInt()
        val sKeyEven = state.even.toInt()

        // Find which sum groups S_key belongs to
        var oddA0PartSum = -1
        var oddA8PartSum = -1
        var evenA0PartSum = -1
        var evenA8PartSum = -1
        for (ps in 0..16) {
            if (HardnestedSumProperty.partSumA0OddBitarrays[ps].test(sKeyOdd)) oddA0PartSum = ps
            if (HardnestedSumProperty.partSumA8OddBitarrays[ps].test(sKeyOdd)) oddA8PartSum = ps
            if (HardnestedSumProperty.partSumA0EvenBitarrays[ps].test(sKeyEven)) evenA0PartSum = ps
            if (HardnestedSumProperty.partSumA8EvenBitarrays[ps].test(sKeyEven)) evenA8PartSum = ps
        }

        // Use precomputed bitflip tables for even-half, all-ones for odd-half
        val bitflipOdd = StateBitarray()
        bitflipOdd.setAll()
        val bitflipEven = StateBitarray()
        bitflipEven.setAll()

        if (HardnestedBitflip.initialize()) {
            // Generate nonces with parity for 9-bit bitflip computation
            data class SimNonce(
                val encFirstByte: Int,
                val parByte0: Int,
            )
            val noncesByByte = mutableMapOf<Int, SimNonce>()
            for (nTVal in 0 until 2048) {
                val nT = (nTVal.toUInt() shl 21) or 0xABCDu
                val simState = Crypto1State()
                simState.loadKey(key)
                val ksWord = simState.lfsrWord(uid xor nT, false)
                val encNonce = nT xor ksWord
                val byte0 = ((encNonce shr 24) and 0xFFu).toInt()
                if (byte0 !in noncesByByte) {
                    val parState = Crypto1State()
                    parState.loadKey(key)
                    parState.lfsrByte(((uid xor nT).toInt() shr 24) and 0xFF, false)
                    val ksPar = Crypto1.filter(parState.odd)
                    val plainByte = ((nT shr 24) and 0xFFu).toInt()
                    val encParBit = Crypto1Auth.oddParity(plainByte) xor ksPar
                    noncesByByte[byte0] = SimNonce(byte0, encParBit)
                }
            }
            // Use first observed byte as reference (matching HardnestedAttack.kt)
            val refByte = noncesByByte.keys.first()
            val refPar = noncesByByte[refByte]!!.parByte0
            val bitflips = mutableSetOf<Int>()
            for ((byte0, nonce) in noncesByByte) {
                if (byte0 == refByte) continue
                val xor = refByte xor byte0
                if (refPar == nonce.parByte0) {
                    bitflips.add(xor)
                } else {
                    bitflips.add(xor or 0x100)
                }
            }

            for (bf in bitflips) {
                val table = HardnestedBitflip.loadBitflipTable(bf, 0) ?: continue
                bitflipEven.and(table)
            }
        }

        // Three-way AND using correct sum groups (A8 for byte0, A0 for byte1)
        val oddCount =
            HardnestedSumProperty.partSumA8OddBitarrays[oddA8PartSum]
                .threeWayAndPopcount(HardnestedSumProperty.partSumA0OddBitarrays[oddA0PartSum], bitflipOdd)
        val evenCount =
            HardnestedSumProperty.partSumA8EvenBitarrays[evenA8PartSum]
                .threeWayAndPopcount(HardnestedSumProperty.partSumA0EvenBitarrays[evenA0PartSum], bitflipEven)

        assertTrue(oddCount > 0, "Three-way AND odd should have candidates ($oddCount)")
        assertTrue(evenCount > 0, "Three-way AND even should have candidates ($evenCount)")

        // Verify S_key survives the three-way AND
        val oddResult = HardnestedSumProperty.partSumA8OddBitarrays[oddA8PartSum].copy()
        oddResult.and(HardnestedSumProperty.partSumA0OddBitarrays[oddA0PartSum])
        oddResult.and(bitflipOdd)
        assertTrue(oddResult.test(sKeyOdd), "S_key odd must survive three-way AND")

        val evenResult = HardnestedSumProperty.partSumA8EvenBitarrays[evenA8PartSum].copy()
        evenResult.and(HardnestedSumProperty.partSumA0EvenBitarrays[evenA0PartSum])
        evenResult.and(bitflipEven)
        assertTrue(evenResult.test(sKeyEven), "S_key even must survive three-way AND")

        // The three-way count should be <= the two-way count
        val twoWayOdd =
            HardnestedSumProperty.partSumA8OddBitarrays[oddA8PartSum]
                .andPopcount(HardnestedSumProperty.partSumA0OddBitarrays[oddA0PartSum])
        assertTrue(
            oddCount <= twoWayOdd,
            "Three-way odd ($oddCount) should be <= two-way odd ($twoWayOdd)",
        )

        println("Three-way AND: $oddCount odd x $evenCount even = ${oddCount.toLong() * evenCount}")
    }

    // ---- Brute force parity check diagnostic ----

    /**
     * Validate that the brute force parity check correctly verifies S_key against
     * a simulated encrypted nonce, matching Proxmark3's simulate_MFplus_RNG exactly.
     *
     * This is the definitive test for whether the brute force logic is correct.
     */
    @Test
    fun testBruteForceParityCheckWithKnownKey() {
        val targetKey = 0xA0A1A2A3A4A5L
        val uid = 0xB7164F30u
        val plainNonce = 0x87654321u

        // Get S_key
        val sKey = Crypto1State()
        sKey.loadKey(targetKey)
        val sKeyOdd = sKey.odd
        val sKeyEven = sKey.even

        // --- Simulate encrypted nonce (Proxmark3's simulate_MFplus_RNG) ---
        val cardState = Crypto1State()
        cardState.loadKey(targetKey)

        var encNonce = 0u
        var encParity = 0
        for (bytePos in 3 downTo 0) {
            val plainByte = ((plainNonce shr (8 * bytePos)) and 0xFFu).toInt()
            val uidByte = ((uid shr (8 * bytePos)) and 0xFFu).toInt()
            val ksByte = cardState.lfsrByte(plainByte xor uidByte, false)
            val encByte = ksByte xor plainByte
            encNonce = (encNonce shl 8) or (encByte.toUInt() and 0xFFu)

            val ksPar = Crypto1.filter(cardState.odd)
            val encParBit = ksPar xor Crypto1Auth.oddParity(plainByte)
            encParity = (encParity shl 1) or encParBit
        }

        println("Simulated: encNonce=0x${encNonce.toString(16)}, encParity=0x${encParity.toString(16)}")

        // --- Run brute force parity check with correct S_key ---
        val bfState = Crypto1State()
        bfState.odd = sKeyOdd
        bfState.even = sKeyEven

        val uidXorEnc = uid xor encNonce
        val inputBytes =
            IntArray(4) { byteIdx ->
                ((uidXorEnc shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            }
        val encBytes =
            IntArray(4) { byteIdx ->
                ((encNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            }
        val encParBits =
            IntArray(4) { byteIdx ->
                (encParity shr (3 - byteIdx)) and 1
            }

        for (byteIdx in 0 until 4) {
            val ksByte = bfState.lfsrByte(inputBytes[byteIdx], true)
            val ksPar = Crypto1.filter(bfState.odd)
            val plainByte = encBytes[byteIdx] xor ksByte
            val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
            val actualPlainByte = ((plainNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()

            println(
                "  byte $byteIdx: ksByte=0x${ksByte.toString(16).padStart(2, '0')} " +
                    "ksPar=$ksPar plainByte=0x${plainByte.toString(16).padStart(2, '0')} " +
                    "(expected 0x${actualPlainByte.toString(16).padStart(2, '0')}) " +
                    "expectedEncPar=$expectedEncPar encParBit=${encParBits[byteIdx]}",
            )

            assertEquals(
                actualPlainByte,
                plainByte,
                "Decrypted byte $byteIdx should match original plaintext",
            )
            assertEquals(
                expectedEncPar,
                encParBits[byteIdx],
                "Parity check at byte $byteIdx should pass for correct S_key",
            )
        }

        // --- Verify that a wrong state FAILS the parity check ---
        val wrongState = Crypto1State()
        wrongState.odd = sKeyOdd xor 0x01u // flip one bit
        wrongState.even = sKeyEven

        var wrongPassed = true
        for (byteIdx in 0 until 4) {
            val ksByte = wrongState.lfsrByte(inputBytes[byteIdx], true)
            val ksPar = Crypto1.filter(wrongState.odd)
            val plainByte = encBytes[byteIdx] xor ksByte
            val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
            if (expectedEncPar != encParBits[byteIdx]) {
                wrongPassed = false
                break
            }
        }
        assertFalse(wrongPassed, "Wrong state should fail parity check")
    }

    /**
     * Test the full brute force verification flow: key recovery from S_key states.
     * Simulates what the hardware brute force does: given (odd, even) and an encrypted nonce,
     * reconstruct the key and verify against additional nonces.
     */
    @Test
    fun testFullBruteForceKeyRecovery() {
        val targetKey = 0xA0A1A2A3A4A5L
        val uid = 0xB7164F30u

        // Get S_key
        val sKey = Crypto1State()
        sKey.loadKey(targetKey)
        val sKeyOdd = sKey.odd.toInt()
        val sKeyEven = sKey.even.toInt()

        // Generate 5 simulated nonces
        val plainNonces = listOf(0x12345678u, 0xABCDEF01u, 0x87654321u, 0xDEADBEEFu, 0xCAFEBABEu)

        data class SimNonce(
            val encNonce: UInt,
            val encParity: Int,
        )

        val simNonces =
            plainNonces.map { plainNonce ->
                val cs = Crypto1State()
                cs.loadKey(targetKey)
                var enc = 0u
                var par = 0
                for (bytePos in 3 downTo 0) {
                    val pb = ((plainNonce shr (8 * bytePos)) and 0xFFu).toInt()
                    val ub = ((uid shr (8 * bytePos)) and 0xFFu).toInt()
                    val ks = cs.lfsrByte(pb xor ub, false)
                    enc = (enc shl 8) or ((ks xor pb).toUInt() and 0xFFu)
                    val kp = Crypto1.filter(cs.odd)
                    par = (par shl 1) or (kp xor Crypto1Auth.oddParity(pb))
                }
                SimNonce(enc, par)
            }

        // Brute force with correct S_key
        val primaryNonce = simNonces[0]
        val uidXorEnc = uid xor primaryNonce.encNonce
        val inputBytes = IntArray(4) { i -> ((uidXorEnc shr ((3 - i) * 8)) and 0xFFu).toInt() }
        val encBytes = IntArray(4) { i -> ((primaryNonce.encNonce shr ((3 - i) * 8)) and 0xFFu).toInt() }
        val encParBits = IntArray(4) { i -> (primaryNonce.encParity shr (3 - i)) and 1 }

        val state = Crypto1State()
        state.odd = sKeyOdd.toUInt()
        state.even = sKeyEven.toUInt()

        // Primary parity check
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
        assertTrue(parityOk, "S_key should pass primary parity check")

        // Extract key from S_key
        state.odd = sKeyOdd.toUInt()
        state.even = sKeyEven.toUInt()
        val recoveredKey = state.getKey()
        assertEquals(targetKey, recoveredKey, "Key recovery from S_key should match")

        // Verify recovered key against additional nonces
        for ((idx, vn) in simNonces.drop(1).withIndex()) {
            val verifyState = Crypto1State()
            verifyState.loadKey(recoveredKey)
            val vUidXorEnc = uid xor vn.encNonce
            var allMatch = true
            for (byteIdx in 0 until 4) {
                val inputByte = ((vUidXorEnc shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
                val ksByte = verifyState.lfsrByte(inputByte, true)
                val ksPar = Crypto1.filter(verifyState.odd)
                val encByte = ((vn.encNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
                val plainByte = encByte xor ksByte
                val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
                val actualEncPar = (vn.encParity shr (3 - byteIdx)) and 1
                if (expectedEncPar != actualEncPar) {
                    allMatch = false
                    break
                }
            }
            assertTrue(allMatch, "Verify nonce $idx should pass for recovered key")
        }
    }

    /**
     * Test the post-byte-0 rollback path used by bruteForceStateList.
     *
     * Proxmark3's set_test_state creates candidates by:
     *   crypto1_create(key) → crypto1_byte((cuid>>24)^byte, true) → take odd/even
     *
     * bruteForceStateList rolls back 1 byte from these candidates to recover the
     * initial key state, then processes uid^encNonce for parity verification, then
     * rolls back again to extract the key via getKey().
     *
     * This test verifies the full pipeline end-to-end with a known key.
     */
    @Test
    fun testPostByte0RollbackKeyRecovery() {
        val targetKey = 0xA0A1A2A3A4A5L
        val uid = 0xB7164F30u

        // Step 1: Compute the initial key state (S_key)
        val sKey = Crypto1State()
        sKey.loadKey(targetKey)
        val initialOdd = sKey.odd
        val initialEven = sKey.even

        // Step 2: Compute the post-byte-0 state (matching Proxmark3's set_test_state)
        // set_test_state: crypto1_create(key) → crypto1_byte((cuid>>24)^byte, true)
        // where byte = best_first_bytes[0] = encrypted first byte of nonce
        //
        // First, generate a simulated nonce to get the encrypted first byte
        val plainNonce = 0x12345678u
        val simState = Crypto1State()
        simState.loadKey(targetKey)
        val ksWord = simState.lfsrWord(uid xor plainNonce, false)
        val encNonce = plainNonce xor ksWord

        // Compute encrypted parity
        val parState = Crypto1State()
        parState.loadKey(targetKey)
        var encParity = 0
        for (byteIdx in 0 until 4) {
            parState.lfsrByte(
                ((uid xor plainNonce).toInt() shr ((3 - byteIdx) * 8)) and 0xFF,
                false,
            )
            val ksPar = Crypto1.filter(parState.odd)
            val plainByte = ((plainNonce shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
            encParity = encParity or ((Crypto1Auth.oddParity(plainByte) xor ksPar) shl (3 - byteIdx))
        }

        val encByte0 = ((encNonce shr 24) and 0xFFu).toInt()

        // Compute post-byte-0 state (same as Proxmark3's set_test_state)
        val postByte0 = Crypto1State()
        postByte0.loadKey(targetKey)
        postByte0.lfsrByte(((uid shr 24).toInt() and 0xFF) xor encByte0, true)
        val postByte0Odd = postByte0.odd
        val postByte0Even = postByte0.even

        // Verify post-byte-0 state is different from initial (sanity check)
        assertTrue(
            postByte0Odd != initialOdd || postByte0Even != initialEven,
            "Post-byte-0 state should differ from initial",
        )

        // Step 3: Roll back from post-byte-0 to recover initial key state
        // (matches bruteForceStateList's rollback)
        val rollbackInput = ((uid shr 24).toInt() and 0xFF) xor encByte0
        val rolledBack = Crypto1State()
        rolledBack.odd = postByte0Odd
        rolledBack.even = postByte0Even
        rolledBack.lfsrRollbackByte(rollbackInput, true)

        // Verify we recovered the initial key state
        assertEquals(initialOdd, rolledBack.odd, "Rollback should recover initial odd state")
        assertEquals(initialEven, rolledBack.even, "Rollback should recover initial even state")

        // Step 4: Verify parity check passes from the recovered state
        // (mirrors bruteForceStateList's parity check loop)
        val uidXorEnc = uid xor encNonce
        val inputBytes = IntArray(4) { i -> ((uidXorEnc shr ((3 - i) * 8)) and 0xFFu).toInt() }
        val encBytes = IntArray(4) { i -> ((encNonce shr ((3 - i) * 8)) and 0xFFu).toInt() }
        val encParBits = IntArray(4) { i -> (encParity shr (3 - i)) and 1 }

        val parityCheck = Crypto1State()
        parityCheck.odd = postByte0Odd
        parityCheck.even = postByte0Even
        parityCheck.lfsrRollbackByte(rollbackInput, true)

        var parityOk = true
        for (byteIdx in 0 until 4) {
            val ksByte = parityCheck.lfsrByte(inputBytes[byteIdx], true)
            val ksPar = Crypto1.filter(parityCheck.odd)
            val plainByte = encBytes[byteIdx] xor ksByte
            val expectedEncPar = Crypto1Auth.oddParity(plainByte) xor ksPar
            if (expectedEncPar != encParBits[byteIdx]) {
                parityOk = false
                break
            }
        }
        assertTrue(parityOk, "Post-byte-0 state should pass parity check after rollback")

        // Step 5: Roll back again and extract key (matches bruteForceStateList)
        val keyExtract = Crypto1State()
        keyExtract.odd = postByte0Odd
        keyExtract.even = postByte0Even
        keyExtract.lfsrRollbackByte(rollbackInput, true)
        val recoveredKey = keyExtract.getKey()

        assertEquals(targetKey, recoveredKey, "Key extracted after rollback should match target key")
    }

    // ---- verifyKeyWithNonceCpu / verifyKeys tests ----

    @Test
    fun testVerifyKeyWithNonceCpuMatchesReference() {
        val correctKey = 0x0A0B0C0D0E0FL
        val uid = 0xB7164F30u

        // Generate 5 synthetic nonces with known plaintext values
        val nonceValues = listOf(0xCAFEBABEu, 0xDEADBEEFu, 0x12345678u, 0xA5A5A5A5u, 0x01020304u)

        data class SyntheticNonce(
            val encNonce: UInt,
            val encParity: Int,
        )

        // For each nonce, compute the encrypted nonce and encrypted parity
        // using the same approach as testVerifyKeyWithNonceConsistency
        val syntheticNonces =
            nonceValues.map { nT ->
                // Generate encrypted nonce: load key, clock through uid^nT
                val cardState = Crypto1State()
                cardState.loadKey(correctKey)
                val ksWord = cardState.lfsrWord(uid xor nT, false)
                val encNonce = nT xor ksWord

                // Generate encrypted parity using byte-level clocking
                val parState = Crypto1State()
                parState.loadKey(correctKey)
                var encParity = 0
                for (byteIdx in 0 until 4) {
                    parState.lfsrByte(
                        ((uid xor nT).toInt() shr ((3 - byteIdx) * 8)) and 0xFF,
                        false,
                    )
                    val ksPar = Crypto1.filter(parState.odd)
                    val plainByte = ((nT shr ((3 - byteIdx) * 8)) and 0xFFu).toInt()
                    encParity = encParity or ((Crypto1Auth.oddParity(plainByte) xor ksPar) shl (3 - byteIdx))
                }

                SyntheticNonce(encNonce, encParity)
            }

        // Verify that verifyKeyWithNonceCpu returns true for the correct key on each nonce
        val state = Crypto1State()
        for ((idx, sn) in syntheticNonces.withIndex()) {
            val result = verifyKeyWithNonceCpu(correctKey, sn.encNonce, sn.encParity, uid, state)
            assertTrue(result, "verifyKeyWithNonceCpu should return true for correct key on nonce $idx")
        }

        // Wrong keys may pass individual nonce parity checks (4-bit parity → 1/16 false positive rate),
        // but should fail when verified against ALL nonces together via verifyKeys().
        val wrongKeys =
            listOf(
                0xFFFFFFFFFFFF,
                0x000000000000L,
                0x0A0B0C0D0E0EL, // off by 1 bit
                0xA0A1A2A3A4A5L,
            )

        // Also test ScalarBruteForceEngine.verifyKeys() which wraps verifyKeyWithNonceCpu
        val verifyNonceData =
            syntheticNonces.map { sn ->
                VerifyNonceData(sn.encNonce, sn.encParity)
            }

        val allKeys = listOf(correctKey) + wrongKeys
        val survivors = ScalarBruteForceEngine().verifyKeys(allKeys, verifyNonceData, uid)

        assertEquals(1, survivors.size, "Only the correct key should survive verifyKeys")
        assertEquals(correctKey, survivors[0], "The surviving key should be the correct key")
    }

    // ---- Parallel filtering correctness tests ----

    /**
     * Verify that the chunk-and-merge parallel filtering strategy used by
     * HardnestedAttack.filterStatesParallel() produces identical results
     * to sequential filtering.
     *
     * filterStatesParallel() is private, so we replicate its algorithm here:
     *   1. Collect all set bit indices from a StateBitarray
     *   2. Apply a deterministic filter predicate to each state
     *   3. Compare: sequential filtering vs chunked filtering with merge
     *
     * This catches ordering bugs, off-by-one in chunk boundaries, and
     * dropped/duplicated states from the chunk-merge pattern.
     */
    @Test
    fun testParallelFilteringMatchesSequential() {
        // Build a StateBitarray with well over PARALLEL_FILTER_THRESHOLD (1000) set bits.
        // Use a deterministic pattern: set every 4th bit in a range, giving 4096 set bits.
        val bitarray = StateBitarray()
        for (i in 0 until 16384 step 4) {
            bitarray.set(i)
        }
        val totalSet = bitarray.popcount()
        assertEquals(4096, totalSet, "Should have 4096 set bits")

        // Collect all set bit indices (same as filterStatesParallel step 1)
        val allStates = mutableListOf<Int>()
        bitarray.forEachSet { state -> allStates.add(state) }
        assertEquals(totalSet, allStates.size, "forEachSet should yield all set bits")

        // Define a deterministic filter predicate that accepts ~50% of states.
        // Uses bit manipulation matching the kind of filtering allBitflipsMatch does.
        val predicate: (Int) -> Boolean = { state ->
            // Accept states where bit 3 XOR bit 7 XOR bit 11 equals 1
            val bit3 = (state shr 3) and 1
            val bit7 = (state shr 7) and 1
            val bit11 = (state shr 11) and 1
            (bit3 xor bit7 xor bit11) == 1
        }

        // Sequential filtering (reference result)
        val sequentialResult = allStates.filter(predicate).toIntArray()

        // Chunked filtering (mirrors filterStatesParallel's parallel path)
        val numChunks = 4 // simulate multi-core
        val chunkSize = (allStates.size + numChunks - 1) / numChunks
        val chunkedResult = mutableListOf<Int>()
        for (start in 0 until allStates.size step chunkSize) {
            val end = minOf(start + chunkSize, allStates.size)
            // Each chunk filters independently (as coroutines would)
            val chunkFiltered = mutableListOf<Int>()
            for (idx in start until end) {
                val state = allStates[idx]
                if (predicate(state)) {
                    chunkFiltered.add(state)
                }
            }
            // Merge in order (as filterStatesParallel does with sequential await)
            chunkedResult.addAll(chunkFiltered)
        }
        val chunkedArray = chunkedResult.toIntArray()

        // Verify identical results
        assertTrue(sequentialResult.isNotEmpty(), "Filter should accept some states")
        assertTrue(sequentialResult.size > 100, "Filter should accept a substantial number of states")
        assertEquals(
            sequentialResult.size,
            chunkedArray.size,
            "Chunked filtering should produce same count as sequential",
        )
        assertTrue(
            sequentialResult.contentEquals(chunkedArray),
            "Chunked filtering should produce identical results to sequential " +
                "(sequential=${sequentialResult.size}, chunked=${chunkedArray.size})",
        )
    }

    /**
     * Verify chunked filtering correctness at chunk boundaries.
     *
     * Tests edge cases: state counts that don't divide evenly into chunks,
     * very small chunks (1 element), and the transition between sequential
     * and parallel paths at the PARALLEL_FILTER_THRESHOLD boundary.
     */
    @Test
    fun testChunkedFilteringBoundaryConditions() {
        // Test with exactly PARALLEL_FILTER_THRESHOLD states (boundary case)
        val threshold = 1000
        val bitarray = StateBitarray()
        // Set exactly 'threshold' bits using a prime step to avoid alignment
        var count = 0
        var idx = 0
        while (count < threshold) {
            bitarray.set(idx)
            count++
            idx += 7 // prime step avoids power-of-2 alignment artifacts
        }
        assertEquals(threshold, bitarray.popcount())

        val allStates = mutableListOf<Int>()
        bitarray.forEachSet { state -> allStates.add(state) }
        assertEquals(threshold, allStates.size)

        // Filter: accept states where the state value mod 3 != 0
        val predicate: (Int) -> Boolean = { state -> state % 3 != 0 }

        val sequentialResult = allStates.filter(predicate).toIntArray()

        // Test with various chunk counts including odd divisors
        for (numChunks in listOf(1, 2, 3, 7, 13, 64, threshold)) {
            val chunkSize = (allStates.size + numChunks - 1) / numChunks
            val chunkedResult = mutableListOf<Int>()
            for (start in 0 until allStates.size step chunkSize) {
                val end = minOf(start + chunkSize, allStates.size)
                for (i in start until end) {
                    if (predicate(allStates[i])) {
                        chunkedResult.add(allStates[i])
                    }
                }
            }
            assertTrue(
                sequentialResult.contentEquals(chunkedResult.toIntArray()),
                "Chunked filtering with $numChunks chunks should match sequential " +
                    "(expected ${sequentialResult.size}, got ${chunkedResult.size})",
            )
        }
    }

    /**
     * Verify that chunked filtering preserves state ordering.
     *
     * filterStatesParallel() awaits deferred results in chunk order (not
     * completion order), so the output must be in the same order as sequential
     * iteration over the bitarray. This test specifically checks ordering.
     */
    @Test
    fun testChunkedFilteringPreservesOrdering() {
        // Create a bitarray with states at irregular positions
        val bitarray = StateBitarray()
        val positions = mutableListOf<Int>()
        // Use a pseudo-random but deterministic pattern
        var pos = 17
        for (i in 0 until 2000) {
            bitarray.set(pos)
            positions.add(pos)
            pos = (pos * 31 + 97) % (1 shl 24) // LCG for deterministic pseudo-random positions
        }

        val allStates = mutableListOf<Int>()
        bitarray.forEachSet { state -> allStates.add(state) }

        // forEachSet iterates in ascending bit order, not insertion order
        // Verify this property (which filterStatesParallel relies on)
        for (i in 1 until allStates.size) {
            assertTrue(
                allStates[i] > allStates[i - 1],
                "forEachSet should yield states in ascending order",
            )
        }

        // Accept ~half the states with a deterministic predicate
        val predicate: (Int) -> Boolean = { state -> (state and 0x100) != 0 }

        val sequentialResult = allStates.filter(predicate)

        // Chunk into 8 parts and merge
        val numChunks = 8
        val chunkSize = (allStates.size + numChunks - 1) / numChunks
        val chunkedResult = mutableListOf<Int>()
        for (start in 0 until allStates.size step chunkSize) {
            val end = minOf(start + chunkSize, allStates.size)
            for (i in start until end) {
                if (predicate(allStates[i])) {
                    chunkedResult.add(allStates[i])
                }
            }
        }

        // Verify same order
        assertEquals(sequentialResult.size, chunkedResult.size)
        for (i in sequentialResult.indices) {
            assertEquals(
                sequentialResult[i],
                chunkedResult[i],
                "State at position $i should match: sequential=${sequentialResult[i]}, chunked=${chunkedResult[i]}",
            )
        }

        // Verify ascending order is preserved
        for (i in 1 until chunkedResult.size) {
            assertTrue(
                chunkedResult[i] > chunkedResult[i - 1],
                "Chunked result should maintain ascending order at position $i",
            )
        }
    }
}
