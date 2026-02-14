/*
 * IosNfcFTechnology.kt
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

import platform.CoreNFC.NFCFeliCaTagProtocol

/**
 * iOS implementation of [NfcFTechnology] wrapping Core NFC's [NFCFeliCaTag].
 *
 * FeliCa cards (Suica, PASMO, ICOCA, Edy, etc.) are fully supported by iOS Core NFC.
 * The [NFCFeliCaTag] protocol provides the system code and supports polling,
 * service enumeration, and block reads used by the FeliCa tag reader.
 */
class IosNfcFTechnology(
    private val tag: NFCFeliCaTagProtocol,
) : NfcFTechnology {
    private var _isConnected = false

    override fun connect() {
        _isConnected = true
    }

    override fun close() {
        _isConnected = false
    }

    override val isConnected: Boolean
        get() = _isConnected

    override val systemCode: ByteArray
        get() = tag.currentSystemCode.toByteArray()
}
