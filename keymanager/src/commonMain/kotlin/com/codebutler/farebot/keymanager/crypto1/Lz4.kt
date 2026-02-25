/*
 * Lz4.kt
 *
 * Minimal LZ4 block format decompressor. Supports only the block format
 * (not the frame format with magic number / checksums).
 *
 * LZ4 block format: sequence of (literal_length, literals, match_offset, match_length) tokens.
 * Reference: https://github.com/lz4/lz4/blob/dev/doc/lz4_Block_format.md
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

object Lz4 {
    /**
     * Decompress an LZ4 block.
     *
     * @param src Source buffer containing compressed data
     * @param srcOffset Start offset within [src]
     * @param srcSize Number of compressed bytes
     * @param dstSize Expected decompressed size (must be known in advance)
     * @return Decompressed data
     * @throws IllegalStateException if decompression fails
     */
    fun decompressBlock(
        src: ByteArray,
        srcOffset: Int,
        srcSize: Int,
        dstSize: Int,
    ): ByteArray {
        val dst = ByteArray(dstSize)
        var sIdx = srcOffset
        var dIdx = 0
        val srcEnd = srcOffset + srcSize

        while (sIdx < srcEnd) {
            // Read token byte
            val token = src[sIdx++].toInt() and 0xFF
            var literalLen = token ushr 4
            val matchLenBase = token and 0x0F

            // Read literal length extensions
            if (literalLen == 15) {
                while (sIdx < srcEnd) {
                    val ext = src[sIdx++].toInt() and 0xFF
                    literalLen += ext
                    if (ext < 255) break
                }
            }

            // Copy literals
            check(sIdx + literalLen <= srcEnd) { "LZ4: literal overrun" }
            check(dIdx + literalLen <= dstSize) { "LZ4: output overrun on literal" }
            src.copyInto(dst, dIdx, sIdx, sIdx + literalLen)
            sIdx += literalLen
            dIdx += literalLen

            // Last sequence has no match
            if (sIdx >= srcEnd) break

            // Read 2-byte little-endian match offset
            val offset = (src[sIdx].toInt() and 0xFF) or ((src[sIdx + 1].toInt() and 0xFF) shl 8)
            sIdx += 2
            check(offset > 0) { "LZ4: zero match offset" }

            // Read match length (base + 4 + extensions)
            var matchLen = matchLenBase + 4
            if (matchLenBase == 15) {
                while (sIdx < srcEnd) {
                    val ext = src[sIdx++].toInt() and 0xFF
                    matchLen += ext
                    if (ext < 255) break
                }
            }

            // Copy match (may overlap for RLE patterns, so copy byte-by-byte)
            val matchStart = dIdx - offset
            check(matchStart >= 0) { "LZ4: match offset underrun" }
            check(dIdx + matchLen <= dstSize) { "LZ4: output overrun on match" }
            for (i in 0 until matchLen) {
                dst[dIdx + i] = dst[matchStart + i]
            }
            dIdx += matchLen
        }

        check(dIdx == dstSize) { "LZ4: output size mismatch (got $dIdx, expected $dstSize)" }
        return dst
    }
}
