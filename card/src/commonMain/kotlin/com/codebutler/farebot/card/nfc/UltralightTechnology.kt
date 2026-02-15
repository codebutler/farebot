/*
 * UltralightTechnology.kt
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

package com.codebutler.farebot.card.nfc

interface UltralightTechnology : NfcTechnology {
    val type: Int

    fun readPages(pageOffset: Int): ByteArray

    /**
     * Sends raw data to the card and receives the response.
     * Used for protocol-level commands like GET_VERSION and AUTH_1.
     */
    fun transceive(data: ByteArray): ByteArray

    /**
     * Reconnects to the card after a disconnect.
     * Some protocol commands (GET_VERSION, AUTH_1) may cause the card to disconnect.
     */
    fun reconnect() {
        // Default implementation: close and connect
        close()
        connect()
    }

    companion object {
        const val PAGE_SIZE = 4
        const val TYPE_ULTRALIGHT = 1
        const val TYPE_ULTRALIGHT_C = 2
    }
}
