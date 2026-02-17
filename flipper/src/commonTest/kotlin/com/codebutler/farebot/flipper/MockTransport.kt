/*
 * MockTransport.kt
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

class MockTransport : FlipperTransport {
    val writtenData = mutableListOf<ByteArray>()
    private val responseBuffer = mutableListOf<Byte>()
    private var _connected = false

    override val isConnected: Boolean get() = _connected

    override suspend fun connect() {
        _connected = true
    }

    override suspend fun close() {
        _connected = false
    }

    override suspend fun write(data: ByteArray) {
        writtenData.add(data.copyOf())
    }

    override suspend fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        if (responseBuffer.isEmpty()) return 0
        val toCopy = minOf(length, responseBuffer.size)
        for (i in 0 until toCopy) {
            buffer[offset + i] = responseBuffer.removeFirst()
        }
        return toCopy
    }

    fun enqueueResponse(data: ByteArray) {
        responseBuffer.addAll(data.toList())
    }
}
