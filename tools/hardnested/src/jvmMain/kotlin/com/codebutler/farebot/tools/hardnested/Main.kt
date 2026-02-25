/*
 * Main.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * Packs Proxmark3's precomputed hardnested bitflip tables (LZ4 compressed)
 * into HBFT (Hardnested BitFlip Table) binary files.
 *
 * Reads from the local Proxmark3 repository and produces two HBFT files:
 *   - hardnested_bitflip_even.bin (bitflip_0_* tables, even-half LFSR states)
 *   - hardnested_bitflip_odd.bin  (bitflip_1_* tables, odd-half LFSR states)
 *
 * HBFT format:
 *   [4 bytes]  magic: "HBFT"
 *   [2 bytes]  version: 1 (uint16 LE)
 *   [2 bytes]  count: N (uint16 LE)
 *   [N x 6 bytes] index entries:
 *     [2 bytes]  bitflip_value (uint16 LE)
 *     [4 bytes]  compressed_size (uint32 LE) — raw LZ4 block size
 *   [variable]  concatenated raw LZ4 block data (in index order)
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

package com.codebutler.farebot.tools.hardnested

import java.io.ByteArrayOutputStream
import java.io.File

private const val TABLES_DIR =
    "proxmark3/client/resources/hardnested_tables"

private const val OUTPUT_DIR =
    "keymanager/src/commonMain/composeResources/files"

private val MAGIC = byteArrayOf(0x48, 0x42, 0x46, 0x54) // "HBFT" as raw bytes
private const val VERSION = 1

// LZ4 frame magic number
private const val LZ4_FRAME_MAGIC = 0x184D2204

fun main() {
    packTables(oddEven = 0, label = "even", outputName = "hardnested_bitflip_even.bin")
    packTables(oddEven = 1, label = "odd", outputName = "hardnested_bitflip_odd.bin")
}

private fun packTables(
    oddEven: Int,
    label: String,
    outputName: String,
) {
    println("Packing $label-half bitflip tables (bitflip_${oddEven}_*)...")

    val tablesDir = File(TABLES_DIR)
    if (!tablesDir.exists()) {
        System.err.println("Tables directory not found: $TABLES_DIR")
        System.err.println("Run from the project root directory.")
        return
    }

    val entries = mutableListOf<Pair<Int, ByteArray>>() // (bitflip_value, raw_block_data)

    for (bitflip in 0x001..0x3FF) {
        val fileName = "bitflip_${oddEven}_${"%03x".format(bitflip)}_states.bin.lz4"
        val file = File(tablesDir, fileName)
        if (!file.exists()) continue

        val frameData = file.readBytes()
        val rawBlock = extractLz4Block(frameData)
        if (rawBlock == null) {
            println("  WARNING: Failed to parse LZ4 frame in $fileName")
            continue
        }
        println("  $fileName: ${frameData.size} -> ${rawBlock.size} bytes")
        entries.add(bitflip to rawBlock)
    }

    println("  ${entries.size} tables")

    // Build HBFT binary
    val baos = ByteArrayOutputStream()

    // Header
    baos.write(MAGIC)
    baos.writeLe16(VERSION)
    baos.writeLe16(entries.size)

    // Index entries
    for ((bitflip, data) in entries) {
        baos.writeLe16(bitflip)
        baos.writeLe32(data.size)
    }

    // Raw LZ4 block data
    for ((_, data) in entries) {
        baos.write(data)
    }

    val output = File(OUTPUT_DIR, outputName)
    output.parentFile.mkdirs()
    output.writeBytes(baos.toByteArray())

    println("  Wrote ${output.length()} bytes to ${output.path}")
}

/**
 * Extract the raw LZ4 block data from an LZ4 frame.
 *
 * LZ4 frame format:
 *   [4 bytes] magic: 0x04224D18
 *   [1 byte]  FLG: flags (version, block independence, checksums, content size, dict)
 *   [1 byte]  BD: block descriptor (max block size)
 *   [8 bytes] content size (optional, if FLG bit 3 set)
 *   [4 bytes] dictionary ID (optional, if FLG bit 0 set)
 *   [1 byte]  header checksum
 *   [4 bytes] block size (LE, MSB=0 compressed, MSB=1 uncompressed)
 *   [N bytes] block data
 *   [4 bytes] end mark (0x00000000)
 *   [4 bytes] content checksum (optional)
 *
 * We only need the first (and typically only) block's data.
 */
private fun extractLz4Block(frame: ByteArray): ByteArray? {
    if (frame.size < 11) return null

    // Verify frame magic
    val magic = readLe32(frame, 0)
    if (magic != LZ4_FRAME_MAGIC) return null

    val flg = frame[4].toInt() and 0xFF
    val hasContentSize = (flg shr 3) and 1 == 1
    val hasDictId = flg and 1 == 1

    var offset = 6 // past magic + FLG + BD
    if (hasContentSize) offset += 8
    if (hasDictId) offset += 4
    offset += 1 // header checksum

    // Read block header
    if (offset + 4 > frame.size) return null
    val blockHeader = readLe32(frame, offset)
    offset += 4

    val blockSize = blockHeader and 0x7FFFFFFF

    if (blockSize == 0) return null // end mark
    if (offset + blockSize > frame.size) return null

    return frame.copyOfRange(offset, offset + blockSize)
}

private fun readLe32(
    data: ByteArray,
    offset: Int,
): Int =
    (data[offset].toInt() and 0xFF) or
        ((data[offset + 1].toInt() and 0xFF) shl 8) or
        ((data[offset + 2].toInt() and 0xFF) shl 16) or
        ((data[offset + 3].toInt() and 0xFF) shl 24)

private fun ByteArrayOutputStream.writeLe16(value: Int) {
    write(value and 0xFF)
    write((value shr 8) and 0xFF)
}

private fun ByteArrayOutputStream.writeLe32(value: Int) {
    write(value and 0xFF)
    write((value shr 8) and 0xFF)
    write((value shr 16) and 0xFF)
    write((value shr 24) and 0xFF)
}
