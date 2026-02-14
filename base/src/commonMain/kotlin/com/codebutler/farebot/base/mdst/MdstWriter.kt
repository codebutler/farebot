/*
 * MdstWriter.kt
 * Writer for Metrodroid Station Table (MdST) files.
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid mdst.py
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

package com.codebutler.farebot.base.mdst

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
class MdstWriter(
    version: Long,
    operators: Map<Int, Operator> = emptyMap(),
    lines: Map<Int, Line> = emptyMap(),
    localLanguages: List<String> = emptyList(),
    ttsHintLanguage: String = "",
    licenseNotice: String = "",
) {
    private val buffer = GrowableByteBuffer()
    private val stationOffsets = LinkedHashMap<Int, Int>()
    private val stationsStartOffset: Int

    init {
        val stationDb =
            StationDb(
                version = version,
                localLanguages = localLanguages,
                operators = operators.entries.sortedBy { it.key }.associate { it.toPair() },
                lines = lines.entries.sortedBy { it.key }.associate { it.toPair() },
                ttsHintLanguage = ttsHintLanguage,
                licenseNotice = licenseNotice,
            )

        // Write magic
        buffer.write(byteArrayOf(0x4d, 0x64, 0x53, 0x54)) // "MdST"
        // Write version
        buffer.writeUint32BE(SCHEMA_VER)
        // Write placeholder for stations_length (will patch later)
        buffer.writeUint32BE(0)
        // Write StationDb header
        buffer.writeDelimited(ProtoBuf.encodeToByteArray(stationDb))

        stationsStartOffset = buffer.size
    }

    fun pushStation(station: MdstStation) {
        stationOffsets[station.id] = buffer.size - stationsStartOffset
        buffer.writeDelimited(ProtoBuf.encodeToByteArray(station))
    }

    fun toByteArray(): ByteArray {
        val stationsLength = buffer.size - stationsStartOffset

        // Write station index using manual protobuf encoding
        // (matching the custom parser in MdstStationTableReader)
        buffer.writeDelimited(encodeStationIndex(stationOffsets))

        val result = buffer.toByteArray()

        // Patch stations_length at offset 8
        result[8] = ((stationsLength ushr 24) and 0xFF).toByte()
        result[9] = ((stationsLength ushr 16) and 0xFF).toByte()
        result[10] = ((stationsLength ushr 8) and 0xFF).toByte()
        result[11] = (stationsLength and 0xFF).toByte()

        return result
    }

    companion object {
        private const val SCHEMA_VER = 1

        internal fun encodeVarint(value: Int): ByteArray {
            val out = mutableListOf<Byte>()
            var v = value
            while (v and 0x7F.inv() != 0) {
                out.add(((v and 0x7F) or 0x80).toByte())
                v = v ushr 7
            }
            out.add((v and 0x7F).toByte())
            return out.toByteArray()
        }

        internal fun encodeStationIndex(stationMap: Map<Int, Int>): ByteArray {
            val out = GrowableByteBuffer()
            for ((key, value) in stationMap.entries.sortedBy { it.key }.associate { it.toPair() }) {
                // Each map entry is field 1, wire type 2 (length-delimited)
                val entry = GrowableByteBuffer()
                // field 1 = key (varint)
                entry.write(encodeVarint((1 shl 3) or 0)) // tag: field 1, wire type 0
                entry.write(encodeVarint(key))
                // field 2 = value (varint) — omit if 0 (proto3 default)
                if (value != 0) {
                    entry.write(encodeVarint((2 shl 3) or 0)) // tag: field 2, wire type 0
                    entry.write(encodeVarint(value))
                }
                val entryBytes = entry.toByteArray()

                // Write map entry as field 1, wire type 2
                out.write(encodeVarint((1 shl 3) or 2)) // tag: field 1, wire type 2
                out.write(encodeVarint(entryBytes.size))
                out.write(entryBytes)
            }
            return out.toByteArray()
        }
    }
}

internal class GrowableByteBuffer {
    private var data = ByteArray(1024)
    var size: Int = 0
        private set

    fun write(bytes: ByteArray) {
        ensureCapacity(size + bytes.size)
        bytes.copyInto(data, size)
        size += bytes.size
    }

    fun write(byte: Int) {
        ensureCapacity(size + 1)
        data[size] = byte.toByte()
        size++
    }

    fun writeUint32BE(value: Int) {
        write((value ushr 24) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }

    fun writeDelimited(bytes: ByteArray) {
        write(MdstWriter.encodeVarint(bytes.size))
        write(bytes)
    }

    fun toByteArray(): ByteArray = data.copyOf(size)

    private fun ensureCapacity(needed: Int) {
        if (needed <= data.size) return
        var newSize = data.size
        while (newSize < needed) newSize *= 2
        data = data.copyOf(newSize)
    }
}
