/*
 * Crypto1.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * Faithful port of crapto1 by bla <blapost@gmail.com>
 * Original: crypto1.c, crapto1.c, crapto1.h from mfcuk/mfoc
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

/**
 * Crypto1 48-bit LFSR stream cipher used in MIFARE Classic cards.
 *
 * Static utility functions for the cipher: filter function, PRNG,
 * parity computation, and endian swapping.
 *
 * Ported from crapto1 by bla <blapost@gmail.com>.
 */
object Crypto1 {
    /** LFSR feedback polynomial taps — odd half */
    const val LF_POLY_ODD: UInt = 0x29CE5Cu

    /** LFSR feedback polynomial taps — even half */
    const val LF_POLY_EVEN: UInt = 0x870804u

    /**
     * Nonlinear 20-bit to 1-bit filter function.
     *
     * Two-layer Boolean function using lookup tables.
     * Layer 1: 5 lookup tables, each mapping a 4-bit nibble to a single bit.
     * Layer 2: 5-bit result from layer 1 selects one bit from fc constant.
     *
     * Faithfully ported from crapto1.h filter().
     */
    fun filter(x: UInt): Int {
        var f: UInt
        f = (0xf22c0u shr (x.toInt() and 0xf)) and 16u
        f = f or ((0x6c9c0u shr ((x shr 4).toInt() and 0xf)) and 8u)
        f = f or ((0x3c8b0u shr ((x shr 8).toInt() and 0xf)) and 4u)
        f = f or ((0x1e458u shr ((x shr 12).toInt() and 0xf)) and 2u)
        f = f or ((0x0d938u shr ((x shr 16).toInt() and 0xf)) and 1u)
        return ((0xEC57E80Au shr f.toInt()) and 1u).toInt()
    }

    /**
     * MIFARE Classic 16-bit PRNG successor function.
     *
     * Polynomial: x^16 + x^14 + x^13 + x^11 + 1
     * Operates on a 32-bit big-endian packed state.
     * Taps: x>>16 xor x>>18 xor x>>19 xor x>>21
     *
     * Faithfully ported from crypto1.c prng_successor().
     */
    fun prngSuccessor(x: UInt, n: UInt): UInt {
        var state = swapEndian(x)
        var count = n
        while (count-- > 0u) {
            state = state shr 1 or
                ((state shr 16 xor (state shr 18) xor (state shr 19) xor (state shr 21)) shl 31)
        }
        return swapEndian(state)
    }

    /**
     * XOR parity of all bits in a 32-bit value.
     *
     * Uses the nibble-lookup trick: fold to 4 bits, then lookup in 0x6996.
     *
     * Faithfully ported from crapto1.h parity().
     */
    fun parity(x: UInt): UInt {
        var v = x
        v = v xor (v shr 16)
        v = v xor (v shr 8)
        v = v xor (v shr 4)
        return (0x6996u shr (v.toInt() and 0xf)) and 1u
    }

    /**
     * Byte-swap a 32-bit value (reverse byte order).
     *
     * Faithfully ported from crypto1.c SWAPENDIAN macro.
     */
    fun swapEndian(x: UInt): UInt {
        // First swap bytes within 16-bit halves, then swap the halves
        var v = (x shr 8 and 0x00ff00ffu) or ((x and 0x00ff00ffu) shl 8)
        v = (v shr 16) or (v shl 16)
        return v
    }

    /**
     * Extract bit n from value x.
     *
     * Equivalent to crapto1.h BIT(x, n).
     */
    internal fun bit(x: UInt, n: Int): UInt = (x shr n) and 1u

    /**
     * Extract bit n from value x with big-endian byte adjustment.
     *
     * Equivalent to crapto1.h BEBIT(x, n) = BIT(x, n ^ 24).
     */
    internal fun bebit(x: UInt, n: Int): UInt = bit(x, n xor 24)

    /**
     * Extract bit n from a Long (64-bit) value.
     */
    internal fun bit64(x: Long, n: Int): UInt = ((x shr n) and 1L).toUInt()
}

/**
 * Mutable Crypto1 cipher state.
 *
 * Contains the 48-bit LFSR split into two 24-bit halves:
 * [odd] holds bits at odd positions and [even] holds bits at even positions.
 *
 * Ported from crapto1 struct Crypto1State.
 */
class Crypto1State(
    var odd: UInt = 0u,
    var even: UInt = 0u,
) {
    /**
     * Load a 48-bit key into the LFSR.
     *
     * Key bit at position i goes to odd[i/2] if i is odd, even[i/2] if i is even.
     * Key bits are indexed 47 downTo 0.
     *
     * Faithfully ported from crypto1.c crypto1_create().
     * Note: The C code uses BIT(key, (i-1)^7) for odd and BIT(key, i^7) for even,
     * where ^7 reverses the bit order within each byte.
     */
    fun loadKey(key: Long) {
        odd = 0u
        even = 0u
        var i = 47
        while (i > 0) {
            odd = odd shl 1 or Crypto1.bit64(key, (i - 1) xor 7)
            even = even shl 1 or Crypto1.bit64(key, i xor 7)
            i -= 2
        }
    }

    /**
     * Clock LFSR once, returning one keystream bit.
     *
     * Returns the filter output (keystream bit) BEFORE clocking.
     * Feedback = input (optionally XORed with output if [isEncrypted])
     *   XOR parity(odd AND LF_POLY_ODD) XOR parity(even AND LF_POLY_EVEN).
     * Shift: even becomes the new odd, feedback bit enters even MSB.
     *
     * Faithfully ported from crypto1.c crypto1_bit().
     */
    fun lfsrBit(input: Int, isEncrypted: Boolean): Int {
        val ret = Crypto1.filter(odd)

        var feedin: UInt = (ret.toUInt() and (if (isEncrypted) 1u else 0u))
        feedin = feedin xor (if (input != 0) 1u else 0u)
        feedin = feedin xor (Crypto1.LF_POLY_ODD and odd)
        feedin = feedin xor (Crypto1.LF_POLY_EVEN and even)
        even = even shl 1 or Crypto1.parity(feedin)

        // Swap odd and even: s->odd ^= (s->odd ^= s->even, s->even ^= s->odd)
        // This is a three-way XOR swap
        odd = odd xor even
        even = even xor odd
        odd = odd xor even

        return ret
    }

    /**
     * Clock LFSR 8 times, processing one byte.
     *
     * Packs keystream bits LSB first.
     *
     * Faithfully ported from crypto1.c crypto1_byte().
     */
    fun lfsrByte(input: Int, isEncrypted: Boolean): Int {
        var ret = 0
        for (i in 0 until 8) {
            ret = ret or (lfsrBit((input shr i) and 1, isEncrypted) shl i)
        }
        return ret
    }

    /**
     * Clock LFSR 32 times, processing one word.
     *
     * Uses BEBIT (big-endian bit) addressing for input/output.
     * Packs keystream bits LSB first within each byte, big-endian byte order.
     *
     * Faithfully ported from crypto1.c crypto1_word().
     */
    fun lfsrWord(input: UInt, isEncrypted: Boolean): UInt {
        var ret = 0u
        for (i in 0 until 32) {
            ret = ret or (lfsrBit(
                Crypto1.bebit(input, i).toInt(),
                isEncrypted,
            ).toUInt() shl (i xor 24))
        }
        return ret
    }

    /**
     * Reverse one LFSR step, undoing the shift to recover the previous state.
     *
     * Returns the filter output at the recovered state.
     *
     * Faithfully ported from crapto1.c lfsr_rollback_bit().
     */
    fun lfsrRollbackBit(input: Int, isEncrypted: Boolean): Int {
        // Mask odd to 24 bits
        odd = odd and 0xFFFFFFu

        // Swap odd and even (reverse the swap done in lfsrBit)
        odd = odd xor even
        even = even xor odd
        odd = odd xor even

        // Extract LSB of even
        val out: UInt = even and 1u
        // Shift even right by 1
        even = even shr 1

        // Compute feedback (what was at MSB of even before)
        var feedback = out
        feedback = feedback xor (Crypto1.LF_POLY_EVEN and even)
        feedback = feedback xor (Crypto1.LF_POLY_ODD and odd)
        feedback = feedback xor (if (input != 0) 1u else 0u)

        val ret = Crypto1.filter(odd)
        feedback = feedback xor (ret.toUInt() and (if (isEncrypted) 1u else 0u))

        even = even or (Crypto1.parity(feedback) shl 23)

        return ret
    }

    /**
     * Reverse 32 LFSR steps.
     *
     * Processes bits 31 downTo 0, using BEBIT addressing.
     *
     * Faithfully ported from crapto1.c lfsr_rollback_word().
     */
    fun lfsrRollbackWord(input: UInt, isEncrypted: Boolean): UInt {
        var ret = 0u
        for (i in 31 downTo 0) {
            ret = ret or (lfsrRollbackBit(
                Crypto1.bebit(input, i).toInt(),
                isEncrypted,
            ).toUInt() shl (i xor 24))
        }
        return ret
    }

    /**
     * Extract the 48-bit key from the current LFSR state.
     *
     * Interleaves odd and even halves back into a 48-bit key value.
     *
     * Faithfully ported from crypto1.c crypto1_get_lfsr().
     */
    fun getKey(): Long {
        var lfsr = 0L
        for (i in 23 downTo 0) {
            lfsr = lfsr shl 1 or Crypto1.bit(odd, i xor 3).toLong()
            lfsr = lfsr shl 1 or Crypto1.bit(even, i xor 3).toLong()
        }
        return lfsr
    }

    /**
     * Deep copy of this cipher state.
     */
    fun copy(): Crypto1State = Crypto1State(odd, even)
}
