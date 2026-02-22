/*
 * FlipperTransport.kt
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

interface FlipperTransport {
    suspend fun connect()

    suspend fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int

    suspend fun write(data: ByteArray)

    suspend fun close()

    val isConnected: Boolean

    /**
     * Whether this transport requires sending "start_rpc_session" to switch from CLI to protobuf mode.
     * USB serial transports need this; BLE transports do not.
     */
    val requiresRpcSessionInit: Boolean get() = false
}
