/*
 * WebUsbPN533Transport.kt
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
 * WebUSB transport layer for PN533 NFC reader communication.
 *
 * Placeholder implementation — the actual WebUSB JS interop
 * (navigator.usb, USBDevice, transferIn/transferOut) will be
 * implemented in a future phase.
 */
class WebUsbPN533Transport : PN533Transport {
    override fun sendCommand(code: Byte, data: ByteArray, timeoutMs: Int): ByteArray {
        error("WebUSB PN533 transport not yet implemented")
    }

    override fun sendAck() {
        error("WebUSB PN533 transport not yet implemented")
    }

    override fun flush() {
        // no-op
    }

    override fun close() {
        // no-op
    }
}
