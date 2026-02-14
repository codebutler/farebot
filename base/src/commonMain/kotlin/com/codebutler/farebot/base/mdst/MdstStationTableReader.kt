/*
 * MdstStationTableReader.kt
 * Reader for Metrodroid Station Table (MdST) files.
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Metrodroid Station Table (MdST) file reader.
 *
 * Binary format:
 * - 4 bytes: Magic "MdST"
 * - 4 bytes: Version (uint32 big-endian, must be 1)
 * - 4 bytes: stations_len (uint32 big-endian, total bytes of station records)
 * - varint-delimited StationDb protobuf message (header)
 * - varint-delimited Station protobuf messages (repeated)
 * - varint-delimited StationIndex protobuf message (byte offset map)
 */
@OptIn(ExperimentalSerializationApi::class)
class MdstStationTableReader private constructor(
    private val data: ByteArray,
    private val stationDb: StationDb,
    private val stationsStart: Int,
    private val stationsLength: Int,
) {
    private val stationIndex: Map<Int, Int> by lazy {
        val indexStart = stationsStart + stationsLength
        if (indexStart >= data.size) {
            emptyMap()
        } else {
            val (bytes, _) = readDelimitedBytes(data, indexStart)
            parseStationIndex(bytes)
        }
    }

    val notice: String?
        get() = stationDb.licenseNotice.ifEmpty { null }

    val localLanguages: List<String>
        get() = stationDb.localLanguages

    val ttsHintLanguage: String
        get() = stationDb.ttsHintLanguage

    fun getStationById(id: Int): MdstStation? {
        val offset = stationIndex[id] ?: return null
        val absoluteOffset = stationsStart + offset
        if (absoluteOffset >= data.size) return null
        return try {
            val (bytes, _) = readDelimitedBytes(data, absoluteOffset)
            ProtoBuf.decodeFromByteArray<MdstStation>(bytes)
        } catch (e: Exception) {
            null
        }
    }

    fun getOperator(id: Int): Operator? = stationDb.operators[id]

    fun getLine(id: Int): Line? = stationDb.lines[id]

    fun getOperatorDefaultTransport(id: Int): TransportType? {
        val op = stationDb.operators[id] ?: return null
        val transport = op.defaultTransport
        return TransportType.entries.getOrNull(transport)
    }

    fun getLineTransport(id: Int): TransportType? {
        val line = stationDb.lines[id] ?: return null
        val transport = line.transport
        return TransportType.entries.getOrNull(transport)
    }

    companion object {
        private val MAGIC = byteArrayOf(0x4d, 0x64, 0x53, 0x54) // "MdST"
        private const val VERSION = 1

        private val readers = HashMap<String, MdstStationTableReader>()

        fun getReader(dbName: String): MdstStationTableReader? {
            readers[dbName]?.let { return it }

            val bytes =
                try {
                    ResourceAccessor.openMdstFile(dbName)
                } catch (e: Exception) {
                    return null
                } ?: return null

            return try {
                val reader = fromByteArray(bytes)
                readers[dbName] = reader
                reader
            } catch (e: Exception) {
                null
            }
        }

        fun fromByteArray(data: ByteArray): MdstStationTableReader {
            if (data.size < 12) {
                throw InvalidHeaderException("File too small")
            }

            // Validate magic
            for (i in 0 until 4) {
                if (data[i] != MAGIC[i]) {
                    throw InvalidHeaderException("Invalid magic")
                }
            }

            // Read version (big-endian uint32)
            val version = readUint32BE(data, 4)
            if (version != VERSION) {
                throw InvalidHeaderException("Unsupported version: $version")
            }

            // Read stations length (big-endian uint32)
            val stationsLength = readUint32BE(data, 8)

            // Read the StationDb header (varint-delimited protobuf)
            var offset = 12
            val (headerBytes, headerEnd) = readDelimitedBytes(data, offset)
            val stationDb = ProtoBuf.decodeFromByteArray<StationDb>(headerBytes)

            val stationsStart = headerEnd

            return MdstStationTableReader(data, stationDb, stationsStart, stationsLength)
        }

        private fun readUint32BE(
            data: ByteArray,
            offset: Int,
        ): Int =
            ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)

        /**
         * Read a varint-delimited protobuf message from byte array.
         * Returns the message bytes and the offset after the message.
         */
        private fun readDelimitedBytes(
            data: ByteArray,
            offset: Int,
        ): Pair<ByteArray, Int> {
            var pos = offset
            var length = 0
            var shift = 0
            while (pos < data.size) {
                val b = data[pos].toInt() and 0xFF
                pos++
                length = length or ((b and 0x7F) shl shift)
                if (b and 0x80 == 0) break
                shift += 7
            }
            val bytes = data.copyOfRange(pos, pos + length)
            return Pair(bytes, pos + length)
        }

        private inline fun <reified T> readDelimitedProto(
            data: ByteArray,
            offset: Int,
        ): T {
            val (bytes, _) = readDelimitedBytes(data, offset)
            return ProtoBuf.decodeFromByteArray(bytes)
        }

        /**
         * Manually parse StationIndex protobuf.
         *
         * kotlinx.serialization.protobuf fails on proto3 map entries where the value
         * is 0 (default), because proto3 omits default values but kotlinx expects them.
         * This parser handles missing values by defaulting to 0.
         *
         * See: https://github.com/Kotlin/kotlinx.serialization/issues/3113
         *
         * The StationIndex message has one field:
         *   map<uint32, uint32> station_map = 1;
         *
         * Each map entry is encoded as a length-delimited submessage (field 1, wire type 2)
         * containing: field 1 = key (varint), field 2 = value (varint).
         */
        private fun parseStationIndex(bytes: ByteArray): Map<Int, Int> {
            val map = HashMap<Int, Int>()
            var pos = 0
            while (pos < bytes.size) {
                // Read field tag
                val (tag, nextPos) = readVarint(bytes, pos)
                pos = nextPos
                val fieldNumber = tag ushr 3
                val wireType = tag and 0x07

                if (fieldNumber == 1 && wireType == 2) {
                    // Length-delimited map entry
                    val (entryLen, entryStart) = readVarint(bytes, pos)
                    pos = entryStart
                    val entryEnd = pos + entryLen

                    var key = 0
                    var value = 0
                    var entryPos = pos
                    while (entryPos < entryEnd) {
                        val (entryTag, entryNext) = readVarint(bytes, entryPos)
                        entryPos = entryNext
                        val entryField = entryTag ushr 3
                        val entryWire = entryTag and 0x07
                        if (entryWire == 0) { // varint
                            val (v, vNext) = readVarint(bytes, entryPos)
                            entryPos = vNext
                            if (entryField == 1) {
                                key = v
                            } else if (entryField == 2) {
                                value = v
                            }
                        } else {
                            break // unexpected wire type
                        }
                    }
                    map[key] = value
                    pos = entryEnd
                } else {
                    // Skip unknown field
                    pos = skipField(bytes, pos, wireType)
                }
            }
            return map
        }

        private fun readVarint(
            data: ByteArray,
            offset: Int,
        ): Pair<Int, Int> {
            var pos = offset
            var result = 0
            var shift = 0
            while (pos < data.size) {
                val b = data[pos].toInt() and 0xFF
                pos++
                result = result or ((b and 0x7F) shl shift)
                if (b and 0x80 == 0) break
                shift += 7
            }
            return Pair(result, pos)
        }

        private fun skipField(
            data: ByteArray,
            offset: Int,
            wireType: Int,
        ): Int =
            when (wireType) {
                0 -> readVarint(data, offset).second // varint
                1 -> offset + 8 // 64-bit
                2 -> { // length-delimited
                    val (len, start) = readVarint(data, offset)
                    start + len
                }
                5 -> offset + 4 // 32-bit
                else -> data.size // unknown, skip to end
            }
    }

    class InvalidHeaderException(
        message: String,
    ) : Exception(message)
}
