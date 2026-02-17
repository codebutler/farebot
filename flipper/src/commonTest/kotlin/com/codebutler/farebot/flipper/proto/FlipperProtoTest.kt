/*
 * FlipperProtoTest.kt
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
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class FlipperProtoTest {
    @Test
    fun testStorageListRequestRoundTrip() {
        val request = StorageListRequest(path = "/ext/nfc")
        val bytes = ProtoBuf.encodeToByteArray(request)
        val decoded = ProtoBuf.decodeFromByteArray<StorageListRequest>(bytes)
        assertEquals("/ext/nfc", decoded.path)
    }

    @Test
    fun testStorageFileRoundTrip() {
        val file =
            StorageFile(
                type = StorageFileType.FILE,
                name = "card.nfc",
                size = 1234u,
            )
        val bytes = ProtoBuf.encodeToByteArray(file)
        val decoded = ProtoBuf.decodeFromByteArray<StorageFile>(bytes)
        assertEquals("card.nfc", decoded.name)
        assertEquals(1234u, decoded.size)
        assertEquals(StorageFileType.FILE, decoded.type)
    }

    @Test
    fun testStorageListResponseRoundTrip() {
        val response =
            StorageListResponse(
                files =
                    listOf(
                        StorageFile(type = StorageFileType.FILE, name = "card.nfc", size = 100u),
                        StorageFile(type = StorageFileType.DIR, name = "assets", size = 0u),
                    ),
            )
        val bytes = ProtoBuf.encodeToByteArray(response)
        val decoded = ProtoBuf.decodeFromByteArray<StorageListResponse>(bytes)
        assertEquals(2, decoded.files.size)
        assertEquals("card.nfc", decoded.files[0].name)
        assertEquals(StorageFileType.DIR, decoded.files[1].type)
    }

    @Test
    fun testCommandStatusValues() {
        assertEquals(0, CommandStatus.OK.value)
        assertEquals(2, CommandStatus.ERROR_STORAGE_NOT_READY.value)
    }

    @Test
    fun testStorageInfoRoundTrip() {
        val response = StorageInfoResponse(totalSpace = 1000000u, freeSpace = 500000u)
        val bytes = ProtoBuf.encodeToByteArray(response)
        val decoded = ProtoBuf.decodeFromByteArray<StorageInfoResponse>(bytes)
        assertEquals(1000000u, decoded.totalSpace)
        assertEquals(500000u, decoded.freeSpace)
    }

    @Test
    fun testSystemDeviceInfoResponseRoundTrip() {
        val response = SystemDeviceInfoResponse(key = "hardware.model", value = "Flipper Zero")
        val bytes = ProtoBuf.encodeToByteArray(response)
        val decoded = ProtoBuf.decodeFromByteArray<SystemDeviceInfoResponse>(bytes)
        assertEquals("hardware.model", decoded.key)
        assertEquals("Flipper Zero", decoded.value)
    }
}
