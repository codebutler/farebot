/*
 * FlipperRpcClient.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.flipper

import com.codebutler.farebot.flipper.proto.CommandStatus
import com.codebutler.farebot.flipper.proto.StorageFile
import com.codebutler.farebot.flipper.proto.StorageInfoResponse
import com.codebutler.farebot.flipper.proto.StorageListResponse
import com.codebutler.farebot.flipper.proto.StorageReadResponse
import com.codebutler.farebot.flipper.proto.StorageStatResponse
import com.codebutler.farebot.flipper.proto.SystemDeviceInfoResponse
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Flipper Zero RPC client implementing the protobuf-based protocol over a serial transport.
 *
 * The Flipper protocol uses a `Main` wrapper message with `oneof` content. Since
 * kotlinx.serialization.protobuf doesn't support `oneof`, we construct and parse
 * `Main` envelopes manually using raw protobuf field encoding.
 *
 * Protocol flow:
 * 1. Connect transport and enable BLE notifications
 * 2. Send/receive varint-length-prefixed protobuf `Main` messages
 * 3. Correlate responses by command_id
 * 4. Handle multi-part responses (has_next = true)
 */
class FlipperRpcClient(
    private val transport: FlipperTransport,
    private val timeoutMs: Long = 30_000L,
) {
    private var nextCommandId = 1

    /** Connect to the Flipper and verify with a ping. */
    suspend fun connect() {
        transport.connect()
        ping()
    }

    /** Send a ping and wait for the pong response. */
    suspend fun ping() {
        val commandId = nextCommandId++
        sendRequest(commandId, FIELD_SYSTEM_PING_REQUEST, byteArrayOf())
        val response = readMainResponse(commandId)
        checkStatus(response)
    }

    /** Disconnect from the Flipper. */
    suspend fun disconnect() {
        transport.close()
    }

    /** List files in a directory on the Flipper's filesystem. */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun listDirectory(path: String): List<FlipperFileEntry> {
        val commandId = nextCommandId++
        val requestBytes =
            ProtoBuf.encodeToByteArray(
                com.codebutler.farebot.flipper.proto
                    .StorageListRequest(path = path),
            )
        sendRequest(commandId, FIELD_STORAGE_LIST_REQUEST, requestBytes)

        val allFiles = mutableListOf<FlipperFileEntry>()
        var hasNext = true
        while (hasNext) {
            val response = readMainResponse(commandId)
            checkStatus(response)
            hasNext = response.hasNext

            if (response.contentFieldNumber == FIELD_STORAGE_LIST_RESPONSE) {
                if (response.contentBytes.isNotEmpty()) {
                    val listResponse = ProtoBuf.decodeFromByteArray<StorageListResponse>(response.contentBytes)
                    for (file in listResponse.files) {
                        allFiles.add(file.toEntry(path))
                    }
                }
            } else if (response.contentFieldNumber != 0) {
                throw FlipperException("Unexpected response field ${response.contentFieldNumber} for listDirectory")
            }
        }
        return allFiles
    }

    /** Read a file from the Flipper's filesystem. Returns the raw file bytes. */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun readFile(path: String): ByteArray {
        val commandId = nextCommandId++
        val requestBytes =
            ProtoBuf.encodeToByteArray(
                com.codebutler.farebot.flipper.proto
                    .StorageReadRequest(path = path),
            )
        sendRequest(commandId, FIELD_STORAGE_READ_REQUEST, requestBytes)

        val chunks = mutableListOf<ByteArray>()
        var hasNext = true
        while (hasNext) {
            val response = readMainResponse(commandId)
            checkStatus(response)
            hasNext = response.hasNext

            if (response.contentFieldNumber == FIELD_STORAGE_READ_RESPONSE) {
                if (response.contentBytes.isNotEmpty()) {
                    val readResponse = ProtoBuf.decodeFromByteArray<StorageReadResponse>(response.contentBytes)
                    if (readResponse.file.data.isNotEmpty()) {
                        chunks.add(readResponse.file.data)
                    }
                }
            } else if (response.contentFieldNumber != 0) {
                throw FlipperException("Unexpected response field ${response.contentFieldNumber} for readFile")
            }
        }

        // Concatenate all chunks
        val totalSize = chunks.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }
        return result
    }

    /** Stat a file on the Flipper's filesystem. */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun statFile(path: String): StorageFile {
        val commandId = nextCommandId++
        val requestBytes =
            ProtoBuf.encodeToByteArray(
                com.codebutler.farebot.flipper.proto
                    .StorageStatRequest(path = path),
            )
        sendRequest(commandId, FIELD_STORAGE_STAT_REQUEST, requestBytes)

        val response = readMainResponse(commandId)
        checkStatus(response)

        if (response.contentFieldNumber == FIELD_STORAGE_STAT_RESPONSE && response.contentBytes.isNotEmpty()) {
            val statResponse = ProtoBuf.decodeFromByteArray<StorageStatResponse>(response.contentBytes)
            return statResponse.file
        }
        throw FlipperException(CommandStatus.ERROR, "No stat response received")
    }

    /** Get storage info (total/free space) for a filesystem path. */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getStorageInfo(path: String): StorageInfoResponse {
        val commandId = nextCommandId++
        val requestBytes =
            ProtoBuf.encodeToByteArray(
                com.codebutler.farebot.flipper.proto
                    .StorageInfoRequest(path = path),
            )
        sendRequest(commandId, FIELD_STORAGE_INFO_REQUEST, requestBytes)

        val response = readMainResponse(commandId)
        checkStatus(response)

        if (response.contentFieldNumber == FIELD_STORAGE_INFO_RESPONSE && response.contentBytes.isNotEmpty()) {
            return ProtoBuf.decodeFromByteArray<StorageInfoResponse>(response.contentBytes)
        }
        throw FlipperException(CommandStatus.ERROR, "No storage info response received")
    }

    /** Get device info as key-value pairs. Multi-part response. */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getDeviceInfo(): Map<String, String> {
        val commandId = nextCommandId++
        val requestBytes =
            ProtoBuf.encodeToByteArray(
                com.codebutler.farebot.flipper.proto
                    .SystemDeviceInfoRequest(),
            )
        sendRequest(commandId, FIELD_SYSTEM_DEVICE_INFO_REQUEST, requestBytes)

        val info = mutableMapOf<String, String>()
        var hasNext = true
        while (hasNext) {
            val response = readMainResponse(commandId)
            checkStatus(response)
            hasNext = response.hasNext

            if (response.contentFieldNumber == FIELD_SYSTEM_DEVICE_INFO_RESPONSE) {
                if (response.contentBytes.isNotEmpty()) {
                    val devInfo = ProtoBuf.decodeFromByteArray<SystemDeviceInfoResponse>(response.contentBytes)
                    if (devInfo.key.isNotEmpty()) {
                        info[devInfo.key] = devInfo.value
                    }
                }
            } else if (response.contentFieldNumber != 0) {
                throw FlipperException("Unexpected response field ${response.contentFieldNumber} for getDeviceInfo")
            }
        }
        return info
    }

    // --- Internal protocol implementation ---

    private suspend fun sendRequest(
        commandId: Int,
        contentFieldNumber: Int,
        contentBytes: ByteArray,
    ) {
        val envelope = buildMainEnvelope(commandId, contentFieldNumber, contentBytes)
        val framed = frameMessage(envelope)
        transport.write(framed)
    }

    /** Read a complete Main response from the transport, with timeout. */
    private suspend fun readMainResponse(expectedCommandId: Int): ParsedMainResponse =
        withTimeout(timeoutMs) {
            val length = readVarintFromTransport()
            val messageBytes = readExactly(length)
            parseMainEnvelope(messageBytes)
        }

    /** Read a varint from the transport one byte at a time. */
    private suspend fun readVarintFromTransport(): Int {
        var result = 0
        var shift = 0
        val buf = ByteArray(1)
        var zeroReadCount = 0
        while (true) {
            val read = transport.read(buf, 0, 1)
            if (read == 0) {
                zeroReadCount++
                if (zeroReadCount > MAX_ZERO_READS) {
                    throw FlipperException("Transport returned no data (disconnected?)")
                }
                continue
            }
            zeroReadCount = 0
            val b = buf[0].toInt() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
            if (shift > 35) throw FlipperException(CommandStatus.ERROR, "Varint too long")
        }
        return result
    }

    /** Read exactly `length` bytes from the transport. */
    private suspend fun readExactly(length: Int): ByteArray {
        val result = ByteArray(length)
        var offset = 0
        var zeroReadCount = 0
        while (offset < length) {
            val read = transport.read(result, offset, length - offset)
            if (read > 0) {
                offset += read
                zeroReadCount = 0
            } else {
                zeroReadCount++
                if (zeroReadCount > MAX_ZERO_READS) {
                    throw FlipperException("Transport returned no data (disconnected?)")
                }
            }
        }
        return result
    }

    private fun checkStatus(response: ParsedMainResponse) {
        if (response.commandStatus != CommandStatus.OK) {
            throw FlipperException(response.commandStatus)
        }
    }

    /** Parsed representation of a Main protobuf envelope. */
    internal data class ParsedMainResponse(
        val commandId: Int = 0,
        val commandStatus: CommandStatus = CommandStatus.OK,
        val hasNext: Boolean = false,
        val contentFieldNumber: Int = 0,
        val contentBytes: ByteArray = byteArrayOf(),
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ParsedMainResponse) return false
            return commandId == other.commandId &&
                commandStatus == other.commandStatus &&
                hasNext == other.hasNext &&
                contentFieldNumber == other.contentFieldNumber &&
                contentBytes.contentEquals(other.contentBytes)
        }

        override fun hashCode(): Int {
            var result = commandId
            result = 31 * result + commandStatus.hashCode()
            result = 31 * result + hasNext.hashCode()
            result = 31 * result + contentFieldNumber
            result = 31 * result + contentBytes.contentHashCode()
            return result
        }
    }

    companion object {
        private const val MAX_ZERO_READS = 1000

        // Main message content field numbers from flipper.proto
        internal const val FIELD_SYSTEM_PING_REQUEST = 5
        internal const val FIELD_SYSTEM_PING_RESPONSE = 6
        internal const val FIELD_STORAGE_LIST_REQUEST = 7
        internal const val FIELD_STORAGE_LIST_RESPONSE = 8
        internal const val FIELD_STORAGE_READ_REQUEST = 9
        internal const val FIELD_STORAGE_READ_RESPONSE = 10
        internal const val FIELD_STORAGE_STAT_REQUEST = 24
        internal const val FIELD_STORAGE_STAT_RESPONSE = 25
        internal const val FIELD_STORAGE_INFO_REQUEST = 28
        internal const val FIELD_STORAGE_INFO_RESPONSE = 29
        internal const val FIELD_SYSTEM_DEVICE_INFO_REQUEST = 32
        internal const val FIELD_SYSTEM_DEVICE_INFO_RESPONSE = 33

        /** Prepend a varint length prefix to a message. */
        fun frameMessage(data: ByteArray): ByteArray {
            val lengthPrefix = Varint.encode(data.size)
            return lengthPrefix + data
        }

        /**
         * Build a raw protobuf Main envelope.
         *
         * Main message layout (from flipper.proto):
         * - field 1: command_id (uint32, varint)
         * - field 2: command_status (enum, varint)
         * - field 3: has_next (bool, varint)
         * - fields 4+: oneof content (length-delimited)
         */
        fun buildMainEnvelope(
            commandId: Int,
            contentFieldNumber: Int,
            contentBytes: ByteArray,
            hasNext: Boolean = false,
            commandStatus: Int = 0,
        ): ByteArray {
            val buf = mutableListOf<Byte>()

            // Field 1: command_id (wire type 0 = varint), tag = (1 << 3) | 0 = 0x08
            buf.add(0x08.toByte())
            buf.addAll(Varint.encode(commandId).toList())

            // Field 2: command_status (wire type 0 = varint), tag = (2 << 3) | 0 = 0x10
            if (commandStatus != 0) {
                buf.add(0x10.toByte())
                buf.addAll(Varint.encode(commandStatus).toList())
            }

            // Field 3: has_next (wire type 0 = varint), tag = (3 << 3) | 0 = 0x18
            if (hasNext) {
                buf.add(0x18.toByte())
                buf.add(0x01.toByte())
            }

            // Content field (wire type 2 = length-delimited)
            val tag = (contentFieldNumber shl 3) or 2
            buf.addAll(Varint.encode(tag).toList())
            buf.addAll(Varint.encode(contentBytes.size).toList())
            buf.addAll(contentBytes.toList())

            return buf.toByteArray()
        }

        /**
         * Parse a raw protobuf Main envelope into its component fields.
         * Iterates raw protobuf tag+value pairs.
         */
        internal fun parseMainEnvelope(data: ByteArray): ParsedMainResponse {
            var commandId = 0
            var commandStatus = CommandStatus.OK
            var hasNext = false
            var contentFieldNumber = 0
            var contentBytes = byteArrayOf()

            var pos = 0
            while (pos < data.size) {
                // Read field tag (varint)
                val (tagValue, tagLen) = Varint.decode(data, pos)
                pos += tagLen

                val fieldNumber = tagValue ushr 3
                val wireType = tagValue and 0x07

                when (wireType) {
                    0 -> {
                        // Varint
                        val (value, valueLen) = Varint.decode(data, pos)
                        pos += valueLen

                        when (fieldNumber) {
                            1 -> commandId = value
                            2 -> commandStatus = CommandStatus.fromValue(value)
                            3 -> hasNext = value != 0
                        }
                    }
                    2 -> {
                        // Length-delimited
                        val (length, lengthLen) = Varint.decode(data, pos)
                        pos += lengthLen

                        if (fieldNumber >= 4) {
                            // This is a content field (oneof)
                            contentFieldNumber = fieldNumber
                            contentBytes = data.copyOfRange(pos, pos + length)
                        }
                        pos += length
                    }
                    else -> {
                        throw FlipperException("Unknown protobuf wire type $wireType at field $fieldNumber")
                    }
                }
            }

            return ParsedMainResponse(
                commandId = commandId,
                commandStatus = commandStatus,
                hasNext = hasNext,
                contentFieldNumber = contentFieldNumber,
                contentBytes = contentBytes,
            )
        }
    }
}

/** A file entry returned by [FlipperRpcClient.listDirectory]. */
data class FlipperFileEntry(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val path: String,
)

private fun StorageFile.toEntry(parentPath: String): FlipperFileEntry {
    val fullPath = if (parentPath.endsWith("/")) "$parentPath$name" else "$parentPath/$name"
    return FlipperFileEntry(
        name = name,
        isDirectory = type == com.codebutler.farebot.flipper.proto.StorageFileType.DIR,
        size = size.toLong(),
        path = fullPath,
    )
}
