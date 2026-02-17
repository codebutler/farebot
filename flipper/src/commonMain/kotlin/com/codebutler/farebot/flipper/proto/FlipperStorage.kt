/*
 * FlipperStorage.kt
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

package com.codebutler.farebot.flipper.proto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable(with = StorageFileTypeSerializer::class)
enum class StorageFileType(val value: Int) {
    FILE(0),
    DIR(1),
}

internal object StorageFileTypeSerializer : KSerializer<StorageFileType> {
    override val descriptor = PrimitiveSerialDescriptor("StorageFileType", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: StorageFileType) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): StorageFileType {
        val v = decoder.decodeInt()
        return StorageFileType.entries.firstOrNull { it.value == v } ?: StorageFileType.FILE
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StorageFile(
    @ProtoNumber(1) val type: StorageFileType = StorageFileType.FILE,
    @ProtoNumber(2) val name: String = "",
    @ProtoNumber(3) val size: UInt = 0u,
    @ProtoNumber(4) val data: ByteArray = byteArrayOf(),
    @ProtoNumber(5) val md5sum: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StorageFile) return false
        return type == other.type && name == other.name && size == other.size &&
            data.contentEquals(other.data) && md5sum == other.md5sum
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + md5sum.hashCode()
        return result
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StorageInfoRequest(
    @ProtoNumber(1) val path: String = "",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StorageInfoResponse(
    @ProtoNumber(1) val totalSpace: ULong = 0u,
    @ProtoNumber(2) val freeSpace: ULong = 0u,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StorageListRequest(
    @ProtoNumber(1) val path: String = "",
    @ProtoNumber(2) val includeMd5: Boolean = false,
    @ProtoNumber(3) val filterMaxSize: UInt = 0u,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StorageListResponse(
    @ProtoNumber(1) val files: List<StorageFile> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StorageReadRequest(
    @ProtoNumber(1) val path: String = "",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StorageReadResponse(
    @ProtoNumber(1) val file: StorageFile = StorageFile(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StorageStatRequest(
    @ProtoNumber(1) val path: String = "",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StorageStatResponse(
    @ProtoNumber(1) val file: StorageFile = StorageFile(),
)
