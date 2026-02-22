/*
 * FlipperBleTransportBase.kt
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

import kotlinx.coroutines.channels.Channel

/**
 * Abstract base class for BLE-based FlipperTransport implementations.
 * Centralizes UUID constants, read buffering, and flow control parsing
 * shared across Android and iOS BLE transports.
 */
abstract class FlipperBleTransportBase : FlipperTransport {
    companion object {
        const val SERIAL_SERVICE_UUID_STRING = "8fe5b3d5-2e7f-4a98-2a48-7acc60fe0000"

        // Phone reads FROM Flipper (subscribe to notifications)
        const val SERIAL_READ_UUID_STRING = "19ed82ae-ed21-4c9d-4145-228e61fe0000"

        // Phone writes TO Flipper
        const val SERIAL_WRITE_UUID_STRING = "19ed82ae-ed21-4c9d-4145-228e62fe0000"

        // Flow control — Flipper reports available buffer space
        const val SERIAL_FLOW_CONTROL_UUID_STRING = "19ed82ae-ed21-4c9d-4145-228e63fe0000"

        const val CCCD_UUID_STRING = "00002902-0000-1000-8000-00805f9b34fb"

        const val SCAN_TIMEOUT_MS = 15_000L
        const val CONNECT_TIMEOUT_MS = 10_000L

        const val FLIPPER_DEVICE_PREFIX = "Flipper"

        /**
         * Parse a 4-byte big-endian flow control value from a BLE characteristic.
         * Returns the free buffer space as an Int, or null if the data is too short.
         */
        fun parseFlowControl(bytes: ByteArray): Int? {
            if (bytes.size < 4) return null
            return ((bytes[0].toInt() and 0xFF) shl 24) or
                ((bytes[1].toInt() and 0xFF) shl 16) or
                ((bytes[2].toInt() and 0xFF) shl 8) or
                (bytes[3].toInt() and 0xFF)
        }
    }

    protected val receiveChannel = Channel<ByteArray>(Channel.UNLIMITED)
    protected var readBuffer = byteArrayOf()

    /**
     * Called by platform BLE callbacks when data is received from the Flipper.
     */
    protected fun onDataReceived(data: ByteArray) {
        receiveChannel.trySend(data)
    }

    override suspend fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        if (readBuffer.isEmpty()) {
            readBuffer = receiveChannel.receive()
        }
        val bytesToCopy = minOf(readBuffer.size, length)
        readBuffer.copyInto(buffer, offset, 0, bytesToCopy)
        readBuffer = readBuffer.copyOfRange(bytesToCopy, readBuffer.size)
        return bytesToCopy
    }

    override suspend fun close() {
        platformClose()
        readBuffer = byteArrayOf()
        receiveChannel.close()
    }

    /**
     * Platform-specific teardown (disconnect BLE, release resources).
     * Called by [close] before clearing the buffer and channel.
     */
    protected abstract suspend fun platformClose()
}
