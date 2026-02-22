/*
 * Crypto1Test.kt
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

/**
 * Tests for the Crypto1 LFSR stream cipher implementation.
 *
 * Reference values verified against the crapto1 C implementation by bla <blapost@gmail.com>.
 */
class Crypto1Test {
    @Test
    fun testFilterFunction() {
        // Verified against crapto1.h filter() compiled from C reference.
        assertEquals(0, Crypto1.filter(0x00000u))
        assertEquals(0, Crypto1.filter(0x00001u))
        assertEquals(1, Crypto1.filter(0x00002u))
        assertEquals(1, Crypto1.filter(0x00003u))
        assertEquals(1, Crypto1.filter(0x00005u))
        assertEquals(0, Crypto1.filter(0x00008u))
        assertEquals(0, Crypto1.filter(0x00010u))
        assertEquals(0, Crypto1.filter(0x10000u))
        assertEquals(1, Crypto1.filter(0xFFFFFu))
        assertEquals(1, Crypto1.filter(0x12345u))
        assertEquals(1, Crypto1.filter(0xABCDEu))
    }

    @Test
    fun testParity() {
        // Verified against crapto1.h parity() compiled from C reference.
        assertEquals(0u, Crypto1.parity(0u))
        assertEquals(1u, Crypto1.parity(1u))
        assertEquals(1u, Crypto1.parity(2u))
        assertEquals(0u, Crypto1.parity(3u))
        assertEquals(0u, Crypto1.parity(0xFFu))
        assertEquals(1u, Crypto1.parity(0x80u))
        assertEquals(0u, Crypto1.parity(0xFFFFFFFFu))
        assertEquals(1u, Crypto1.parity(0x7FFFFFFFu))
        assertEquals(0u, Crypto1.parity(0xAAAAAAAAu))
        assertEquals(0u, Crypto1.parity(0x55555555u))
        assertEquals(1u, Crypto1.parity(0x12345678u))
    }

    @Test
    fun testPrngSuccessor() {
        // Verified against crypto1.c prng_successor() compiled from C reference.

        // Successor of 0 should be 0 (all zero LFSR stays zero)
        assertEquals(0u, Crypto1.prngSuccessor(0u, 1u))

        // Test advancing by 0 steps returns the same value
        assertEquals(0xAABBCCDDu, Crypto1.prngSuccessor(0xAABBCCDDu, 0u))

        // Test specific known values
        assertEquals(0x8b92ec40u, Crypto1.prngSuccessor(0x12345678u, 32u))
        assertEquals(0xcdd2b112u, Crypto1.prngSuccessor(0x12345678u, 64u))

        // Test that advancing by N and then M steps equals advancing by N+M
        val after32 = Crypto1.prngSuccessor(0x12345678u, 32u)
        val after32Then32 = Crypto1.prngSuccessor(after32, 32u)
        assertEquals(0xcdd2b112u, after32Then32)
    }

    @Test
    fun testPrngSuccessor64() {
        // Verify suc^96(n) == suc^32(suc^64(n))
        // Verified against C reference.
        val n = 0xDEADBEEFu
        val suc96 = Crypto1.prngSuccessor(n, 96u)
        val suc64 = Crypto1.prngSuccessor(n, 64u)
        val suc32of64 = Crypto1.prngSuccessor(suc64, 32u)
        assertEquals(0xe63e7417u, suc96)
        assertEquals(suc96, suc32of64)

        // Also verify with a different starting value
        val n2 = 0x01020304u
        val suc96b = Crypto1.prngSuccessor(n2, 96u)
        val suc64b = Crypto1.prngSuccessor(n2, 64u)
        val suc32of64b = Crypto1.prngSuccessor(suc64b, 32u)
        assertEquals(suc96b, suc32of64b)
    }

    @Test
    fun testLoadKeyAndGetKey() {
        // Verified against crypto1.c crypto1_create + crypto1_get_lfsr compiled from C reference.

        // All-ones key: odd=0xFFFFFF, even=0xFFFFFF
        val state1 = Crypto1State()
        state1.loadKey(0xFFFFFFFFFFFFL)
        assertEquals(0xFFFFFFu, state1.odd)
        assertEquals(0xFFFFFFu, state1.even)
        assertEquals(0xFFFFFFFFFFFFL, state1.getKey())

        // Real-world key: odd=0x33BB33, even=0x08084C
        val state2 = Crypto1State()
        state2.loadKey(0xA0A1A2A3A4A5L)
        assertEquals(0x33BB33u, state2.odd)
        assertEquals(0x08084Cu, state2.even)
        assertEquals(0xA0A1A2A3A4A5L, state2.getKey())

        // Zero key: odd=0, even=0
        val state3 = Crypto1State()
        state3.loadKey(0L)
        assertEquals(0u, state3.odd)
        assertEquals(0u, state3.even)
        assertEquals(0L, state3.getKey())

        // Alternating bits: 0xAAAAAAAAAAAA => odd=0xFFFFFF, even=0x000000
        val state4 = Crypto1State()
        state4.loadKey(0xAAAAAAAAAAAAL)
        assertEquals(0xFFFFFFu, state4.odd)
        assertEquals(0x000000u, state4.even)
        assertEquals(0xAAAAAAAAAAAAL, state4.getKey())

        // Alternating bits (other pattern): 0x555555555555 => odd=0x000000, even=0xFFFFFF
        val state5 = Crypto1State()
        state5.loadKey(0x555555555555L)
        assertEquals(0x000000u, state5.odd)
        assertEquals(0xFFFFFFu, state5.even)
        assertEquals(0x555555555555L, state5.getKey())
    }

    @Test
    fun testLfsrBit() {
        // Verified against crypto1.c crypto1_bit() compiled from C reference.
        // Key 0xFFFFFFFFFFFF produces all-ones odd register, and filter(0xFFFFFF) = 1.
        // All 8 keystream bits should be 1 for this key with zero input.
        val state = Crypto1State()
        state.loadKey(0xFFFFFFFFFFFFL)
        val bits = IntArray(8) { state.lfsrBit(0, false) }
        for (i in 0 until 8) {
            assertEquals(1, bits[i], "Keystream bit $i should be 1 for all-ones key")
        }

        // Verify determinism: same key produces same keystream
        val state2 = Crypto1State()
        state2.loadKey(0xFFFFFFFFFFFFL)
        val bits2 = IntArray(8) { state2.lfsrBit(0, false) }
        for (i in 0 until 8) {
            assertEquals(bits[i], bits2[i], "Keystream bit $i mismatch (determinism)")
        }
    }

    @Test
    fun testLfsrByteConsistency() {
        // lfsrByte should produce the same output as 8 calls to lfsrBit.
        // Verified against C reference: lfsrByte(key=0xA0A1A2A3A4A5, input=0x5A) = 0x30
        val key = 0xA0A1A2A3A4A5L
        val inputByte = 0x5A

        // Method 1: lfsrByte
        val state1 = Crypto1State()
        state1.loadKey(key)
        val byteResult = state1.lfsrByte(inputByte, false)
        assertEquals(0x30, byteResult)

        // Method 2: 8 individual lfsrBit calls
        val state2 = Crypto1State()
        state2.loadKey(key)
        var bitResult = 0
        for (i in 0 until 8) {
            bitResult = bitResult or (state2.lfsrBit((inputByte shr i) and 1, false) shl i)
        }
        assertEquals(byteResult, bitResult, "lfsrByte and manual lfsrBit should produce identical output")
    }

    @Test
    fun testLfsrWordRoundtrip() {
        // Verified against C reference: word output = 0x30794609, rollback restores state.
        val key = 0xA0A1A2A3A4A5L
        val state = Crypto1State()
        state.loadKey(key)

        val initialOdd = state.odd
        val initialEven = state.even

        // Advance 32 steps
        val input = 0x12345678u
        val wordOutput = state.lfsrWord(input, false)
        assertEquals(0x30794609u, wordOutput)

        // Roll back 32 steps
        val rollbackOutput = state.lfsrRollbackWord(input, false)
        assertEquals(0x30794609u, rollbackOutput)

        // State should be restored
        assertEquals(initialOdd, state.odd, "Odd register not restored after rollback")
        assertEquals(initialEven, state.even, "Even register not restored after rollback")
    }

    @Test
    fun testLfsrRollbackBitRestoresState() {
        val key = 0xA0A1A2A3A4A5L
        val state = Crypto1State()
        state.loadKey(key)

        val initialOdd = state.odd
        val initialEven = state.even

        // Advance one step
        state.lfsrBit(1, false)

        // Roll back one step
        state.lfsrRollbackBit(1, false)

        assertEquals(initialOdd, state.odd, "Odd not restored after single rollback")
        assertEquals(initialEven, state.even, "Even not restored after single rollback")
    }

    @Test
    fun testSwapEndian() {
        // Verified against C SWAPENDIAN macro.
        assertEquals(0x78563412u, Crypto1.swapEndian(0x12345678u))
        assertEquals(0x00000000u, Crypto1.swapEndian(0x00000000u))
        assertEquals(0xFFFFFFFFu, Crypto1.swapEndian(0xFFFFFFFFu))
        assertEquals(0x04030201u, Crypto1.swapEndian(0x01020304u))
        assertEquals(0xDDCCBBAAu, Crypto1.swapEndian(0xAABBCCDDu))
    }

    @Test
    fun testCopy() {
        val key = 0xA0A1A2A3A4A5L
        val state = Crypto1State()
        state.loadKey(key)

        val copy = state.copy()
        assertEquals(state.odd, copy.odd)
        assertEquals(state.even, copy.even)

        // Modify original, copy should be unaffected
        state.lfsrBit(0, false)
        assertEquals(key, copy.getKey(), "Copy should be independent of original")
    }

    @Test
    fun testEncryptedMode() {
        // Verified against C reference.
        // In encrypted mode, the output keystream bit is XORed into feedback.
        // Output keystream is the same (filter computed before feedback), but states diverge.
        val key = 0xA0A1A2A3A4A5L

        val stateEnc = Crypto1State()
        stateEnc.loadKey(key)
        val encByte = stateEnc.lfsrByte(0x00, true)

        val stateNoEnc = Crypto1State()
        stateNoEnc.loadKey(key)
        val noEncByte = stateNoEnc.lfsrByte(0x00, false)

        // Output should be the same: 0x70
        assertEquals(0x70, encByte)
        assertEquals(0x70, noEncByte)
        assertEquals(encByte, noEncByte, "Keystream output should be same regardless of encrypted flag")

        // Internal states should differ
        val encKey = stateEnc.getKey()
        val noEncKey = stateNoEnc.getKey()
        assertEquals(0xa1a2a3a4a5f6L, encKey)
        assertEquals(0xa1a2a3a4a586L, noEncKey)
    }
}
