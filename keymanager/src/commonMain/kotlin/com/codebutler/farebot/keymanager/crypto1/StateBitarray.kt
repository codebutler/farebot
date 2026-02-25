/*
 * HardnestedBitarray.kt
 *
 * Based on Proxmark3's hardnested state candidate tracking.
 * Original authors: Ïkjas, piwi, and Proxmark3 contributors.
 * https://github.com/RfidResearchGroup/proxmark3
 *
 * Bitarray for tracking 2^24 LFSR state candidates during the
 * hardnested attack. Each bit represents whether a particular
 * 24-bit LFSR half-state (odd or even) is still a candidate.
 * Ported to Kotlin Multiplatform for FareBot.
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
 * Compact bitarray for 2^24 (16,777,216) state candidates.
 *
 * Backed by an IntArray of 2^19 = 524,288 ints (2 MB).
 * Used to track which 24-bit LFSR half-states are still viable
 * candidates during the hardnested attack's filtering phases.
 */
class StateBitarray {
    /** 2^24 bits = 2^19 ints (each int holds 32 bits). */
    @PublishedApi
    internal val data = IntArray(SIZE_INTS)

    /** Set bit at [index]. */
    fun set(index: Int) {
        data[index ushr 5] = data[index ushr 5] or (1 shl (index and 31))
    }

    /** Clear bit at [index]. */
    fun clear(index: Int) {
        data[index ushr 5] = data[index ushr 5] and (1 shl (index and 31)).inv()
    }

    /** Test whether bit at [index] is set. */
    fun test(index: Int): Boolean = (data[index ushr 5] ushr (index and 31)) and 1 != 0

    /** Set all bits. */
    fun setAll() {
        data.fill(-1) // 0xFFFFFFFF
    }

    /** Clear all bits. */
    fun clearAll() {
        data.fill(0)
    }

    /** Bitwise AND with [other], modifying this bitarray in place. */
    fun and(other: StateBitarray) {
        for (i in data.indices) {
            data[i] = data[i] and other.data[i]
        }
    }

    /** Bitwise OR with [other], modifying this bitarray in place. */
    fun or(other: StateBitarray) {
        for (i in data.indices) {
            data[i] = data[i] or other.data[i]
        }
    }

    /** Count the number of set bits (population count). */
    fun popcount(): Int {
        var count = 0
        for (word in data) {
            count += word.countOneBits()
        }
        return count
    }

    /** Iterate over all set bit indices. */
    inline fun forEachSet(block: (Int) -> Unit) {
        for (wordIdx in data.indices) {
            var word = data[wordIdx]
            if (word == 0) continue
            val base = wordIdx shl 5
            while (word != 0) {
                val bit = word.countTrailingZeroBits()
                block(base + bit)
                word = word and (word - 1) // clear lowest set bit
            }
        }
    }

    /** Count set bits in the AND of this bitarray with [other], without allocating. */
    fun andPopcount(other: StateBitarray): Int {
        var count = 0
        for (i in data.indices) {
            count += (data[i] and other.data[i]).countOneBits()
        }
        return count
    }

    /** Count set bits in the AND of this bitarray with [b] and [c], without allocating. */
    fun threeWayAndPopcount(
        b: StateBitarray,
        c: StateBitarray,
    ): Int {
        var count = 0
        for (i in data.indices) {
            count += (data[i] and b.data[i] and c.data[i]).countOneBits()
        }
        return count
    }

    /** Count set bits in the AND of this bitarray with [b], [c], and [d], without allocating. */
    fun fourWayAndPopcount(
        b: StateBitarray,
        c: StateBitarray,
        d: StateBitarray,
    ): Int {
        var count = 0
        for (i in data.indices) {
            count += (data[i] and b.data[i] and c.data[i] and d.data[i]).countOneBits()
        }
        return count
    }

    /**
     * Coarse AND at 16-bit granularity, modifying this bitarray in place.
     * Returns the popcount of the result.
     *
     * Port of Proxmark3's count_bitarray_low20_AND():
     * For each 16-bit half-word, if the corresponding half-word in [other] is
     * entirely zero, zero out this half-word. Otherwise leave it unchanged.
     * This is weaker than a bit-precise AND but faster.
     */
    fun andLow20(other: StateBitarray): Int {
        var count = 0
        for (i in data.indices) {
            val a = data[i]
            val b = other.data[i]
            // Low 16 bits: if other's low half is zero, zero ours
            val lo = if (b and 0xFFFF == 0) a and 0xFFFF0000.toInt() else a
            // High 16 bits: if other's high half is zero, zero ours
            val result = if (b and 0xFFFF0000.toInt() == 0) lo and 0xFFFF else lo
            data[i] = result
            count += result.countOneBits()
        }
        return count
    }

    /** Create a deep copy of this bitarray. */
    fun copy(): StateBitarray {
        val result = StateBitarray()
        this.data.copyInto(result.data)
        return result
    }

    companion object {
        /** Number of addressable bits: 2^24. */
        const val NUM_BITS = 1 shl 24 // 16,777,216

        /** Number of ints backing the bitarray: 2^24 / 32 = 2^19. */
        const val SIZE_INTS = NUM_BITS ushr 5 // 524,288
    }
}
