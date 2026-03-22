/*
 * HardnestedBitflip.kt
 *
 * Based on Proxmark3's hardnested bitflip table logic.
 * Original authors: Ïkjas, piwi, and Proxmark3 contributors.
 * https://github.com/RfidResearchGroup/proxmark3
 *
 * Bitflip property tables for the hardnested attack on MIFARE Classic.
 * Uses precomputed bitflip tables from Proxmark3. Each table is a 2^24-bit
 * bitarray indicating which LFSR half-states (even or odd) are compatible
 * with a given bitflip value (XOR of two encrypted first bytes).
 * Ported to Kotlin Multiplatform for FareBot.
 *
 * Tables are stored in HBFT (Hardnested BitFlip Table) format:
 *   [4 bytes]  magic: "HBFT" (0x48424654)
 *   [2 bytes]  version: 1 (uint16 LE)
 *   [2 bytes]  count: N (uint16 LE)
 *   [N x 6 bytes] index entries:
 *     [2 bytes]  bitflip_value (uint16 LE)
 *     [4 bytes]  compressed_size (uint32 LE)
 *   [variable]  concatenated LZ4 compressed data (in index order)
 *
 * Each compressed entry decompresses to 2,097,152 bytes (2^24 bits / 8),
 * stored as uint32_t[524288] in little-endian byte order (Proxmark3/x86).
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
 * Precomputed bitflip table loader for constraining LFSR state candidates.
 *
 * Loads both even-half and odd-half bitflip tables from Proxmark3's
 * precomputed data (stored in HBFT format as compose resources).
 */
object HardnestedBitflip {
    private val MAGIC = byteArrayOf(0x48, 0x42, 0x46, 0x54) // "HBFT"
    private const val VERSION = 1

    // Proxmark3 decompressed format: [4-byte count][524288 uint32 bitarray] = 2,097,156 bytes.
    // The first 4 bytes are a uint32 LE count of set bits; bitarray starts at offset 4.
    private const val DECOMPRESSED_SIZE = 4 + StateBitarray.SIZE_INTS * 4 // 2,097,156 bytes
    private const val BITARRAY_OFFSET = 4 // skip the count prefix

    // Tables where 99.01%+ of states are set provide negligible filtering (Proxmark3 threshold)
    private const val IGNORE_BITFLIP_THRESHOLD = 0.9901f

    /** Sentinel value indicating a table was above IGNORE_BITFLIP_THRESHOLD (not worth caching). */
    private val FILTERED_SENTINEL = StateBitarray()

    /** Parsed HBFT file: index + raw data for on-demand decompression. */
    private class HbftFile(
        val index: Map<Int, Pair<Int, Int>>, // bitflip_value -> (data_offset, compressed_size)
        val rawData: ByteArray,
    )

    private var evenFile: HbftFile? = null
    private var oddFile: HbftFile? = null

    /** Whether initialization has been attempted. */
    private var initialized = false

    /**
     * Cache of decompressed bitflip tables, keyed by (oddEven, bitflip) encoded as Long.
     * Values are the canonical (immutable) StateBitarray, or [FILTERED_SENTINEL] if the
     * table was above the IGNORE_BITFLIP_THRESHOLD (returning null to callers).
     * Callers receive a .copy() since they mutate the arrays (AND, etc.).
     */
    private val decompressedCache = HashMap<Long, StateBitarray>()

    /** Encode (oddEven, bitflip) into a cache key. */
    private fun cacheKey(
        oddEven: Int,
        bitflip: Int,
    ): Long = oddEven.toLong() shl 32 or bitflip.toLong()

    /**
     * Initialize by loading both HBFT resources. Call once before [loadBitflipTable].
     * Safe to call multiple times (no-op after first call).
     *
     * @return true if at least even tables were loaded
     */
    fun initialize(): Boolean {
        if (initialized) return evenFile != null
        initialized = true

        evenFile = parseHbft(BitflipTableLoader.loadEvenBitflipResource())
        oddFile = parseHbft(BitflipTableLoader.loadOddBitflipResource())

        return evenFile != null
    }

    /**
     * Load a precomputed bitflip table for the given bitflip value and parity half.
     *
     * Returns a copy of the cached decompressed table. On first call for a given
     * (oddEven, bitflip) pair, decompresses and caches the canonical copy.
     * Subsequent calls return .copy() from the cache, avoiding repeated LZ4
     * decompression and bit-reversal (~5s savings per sector across 18 sectors).
     *
     * @param bitflip XOR of two observed encrypted first bytes (1..0x3FF)
     * @param oddEven 0 for even-half states, 1 for odd-half states
     * @return StateBitarray with compatible states, or null if no table exists
     */
    fun loadBitflipTable(
        bitflip: Int,
        oddEven: Int,
    ): StateBitarray? {
        val file = (if (oddEven == 0) evenFile else oddFile) ?: return null

        val key = cacheKey(oddEven, bitflip)
        val cached = decompressedCache[key]
        if (cached != null) {
            return if (cached === FILTERED_SENTINEL) null else cached.copy()
        }

        // Not in cache — decompress, cache canonical copy, return a copy
        val result = decompressTable(file, bitflip)
        if (result == null) {
            decompressedCache[key] = FILTERED_SENTINEL
            return null
        }
        decompressedCache[key] = result
        return result.copy() // always return a copy since callers mutate the arrays
    }

    /**
     * Clear the decompressed table cache.
     * Useful for freeing memory after attack completes.
     */
    fun resetCache() {
        decompressedCache.clear()
    }

    /**
     * Number of available even-half bitflip tables, or 0 if not initialized.
     */
    val evenTableCount: Int get() = evenFile?.index?.size ?: 0

    /**
     * Number of available odd-half bitflip tables, or 0 if not initialized.
     */
    val oddTableCount: Int get() = oddFile?.index?.size ?: 0

    /**
     * Set of available bitflip values for the given parity half.
     */
    fun availableBitflips(oddEven: Int): Set<Int> = (if (oddEven == 0) evenFile else oddFile)?.index?.keys ?: emptySet()

    private fun parseHbft(raw: ByteArray?): HbftFile? {
        if (raw == null || raw.size < 8) return null

        // Verify header
        if (raw[0] != MAGIC[0] || raw[1] != MAGIC[1] || raw[2] != MAGIC[2] || raw[3] != MAGIC[3]) return null
        val version = readLe16(raw, 4)
        val count = readLe16(raw, 6)
        if (version != VERSION || count <= 0) return null

        val headerSize = 8 + count * 6
        if (raw.size < headerSize) return null

        // Build index
        val idx = mutableMapOf<Int, Pair<Int, Int>>()
        var dataOffset = headerSize
        for (i in 0 until count) {
            val entryOffset = 8 + i * 6
            val bitflip = readLe16(raw, entryOffset)
            val compressedSize = readLe32(raw, entryOffset + 2)
            idx[bitflip] = dataOffset to compressedSize
            dataOffset += compressedSize
        }

        return HbftFile(idx, raw)
    }

    private fun decompressTable(
        file: HbftFile,
        bitflip: Int,
    ): StateBitarray? {
        val (offset, size) = file.index[bitflip] ?: return null

        // Decompress LZ4 block
        val decompressed = Lz4.decompressBlock(file.rawData, offset, size, DECOMPRESSED_SIZE)

        // First 4 bytes = uint32 LE count of set bits (Proxmark3 format)
        val count = readLe32(decompressed, 0)

        // Skip tables where nearly all states are set (Proxmark3's IGNORE_BITFLIP_THRESHOLD)
        if (count.toFloat() / (1 shl 24) >= IGNORE_BITFLIP_THRESHOLD) {
            return null
        }

        // Convert from Proxmark3 format to StateBitarray.
        // Proxmark3 uses MSB-first bit ordering within each uint32:
        //   test_bit24(bitarray, state) → bitarray[state >> 5] & (0x80000000 >> (state & 0x1f))
        // Our StateBitarray uses LSB-first:
        //   test(state) → data[state >> 5] & (1 shl (state & 0x1f))
        // We must reverse bits within each uint32 to convert between conventions.
        val result = StateBitarray()
        for (i in 0 until StateBitarray.SIZE_INTS) {
            val byteOffset = BITARRAY_OFFSET + i * 4
            val leWord =
                (decompressed[byteOffset].toInt() and 0xFF) or
                    ((decompressed[byteOffset + 1].toInt() and 0xFF) shl 8) or
                    ((decompressed[byteOffset + 2].toInt() and 0xFF) shl 16) or
                    ((decompressed[byteOffset + 3].toInt() and 0xFF) shl 24)
            result.data[i] = reverseBits(leWord)
        }
        return result
    }

    /** Reverse all 32 bits of an integer (bit 0 ↔ bit 31, bit 1 ↔ bit 30, etc.) */
    private fun reverseBits(x: Int): Int {
        var v = x
        v = ((v and 0x55555555) shl 1) or ((v ushr 1) and 0x55555555)
        v = ((v and 0x33333333) shl 2) or ((v ushr 2) and 0x33333333)
        v = ((v and 0x0F0F0F0F) shl 4) or ((v ushr 4) and 0x0F0F0F0F)
        v = ((v and 0x00FF00FF) shl 8) or ((v ushr 8) and 0x00FF00FF)
        v = (v shl 16) or (v ushr 16)
        return v
    }

    private fun readLe16(
        data: ByteArray,
        offset: Int,
    ): Int =
        (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8)

    private fun readLe32(
        data: ByteArray,
        offset: Int,
    ): Int =
        (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
}
