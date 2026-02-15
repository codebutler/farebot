/*
 * VicinityTechnology.kt
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

/**
 * Common interface for NFC-V (ISO 15693 Vicinity) technology.
 *
 * Provides low-level transceive operations for communicating with NFC-V tags.
 */
interface VicinityTechnology : NfcTechnology {
    /**
     * The UID (tag identifier) of the NFC-V tag.
     * For NFC-V, this is typically an 8-byte UID.
     */
    val uid: ByteArray

    /**
     * Send a raw command to the tag and receive the response.
     *
     * @param data The command bytes to send
     * @return The response from the tag
     * @throws Exception if communication fails
     */
    fun transceive(data: ByteArray): ByteArray
}
