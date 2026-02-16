/*
 * PN533Transport.kt
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

package com.codebutler.farebot.card.nfc.pn533

/**
 * Platform-specific transport layer for PN533 NFC reader communication.
 * Handles USB frame serialization and bulk I/O.
 */
interface PN533Transport {
    fun sendCommand(
        code: Byte,
        data: ByteArray = byteArrayOf(),
        timeoutMs: Int = 5000,
    ): ByteArray

    fun sendAck()

    fun flush()

    fun close()
}

open class PN533Exception(
    message: String,
) : Exception(message)

class PN533CommandException(
    val errorCode: Int,
) : PN533Exception("PN53x command error: 0x${hexByte(errorCode)}")

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

private fun hexByte(value: Int): String {
    val v = value and 0xFF
    return "${HEX_CHARS[v ushr 4]}${HEX_CHARS[v and 0x0F]}"
}
