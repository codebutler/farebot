/*
 * PN533CardTransceiver.kt
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

import com.codebutler.farebot.card.nfc.CardTransceiver

/**
 * PN533 implementation of [CardTransceiver] for ISO-DEP protocols.
 *
 * Uses [PN533.inDataExchange] which handles ISO-DEP (ISO 14443-4)
 * framing automatically for activated targets.
 *
 * Used for: DESFire, CEPAS, ISO 7816
 */
class PN533CardTransceiver(
    private val pn533: PN533,
    private val tg: Int,
) : CardTransceiver {
    private var connected = true

    override fun connect() {
        connected = true
    }

    override fun close() {
        connected = false
    }

    override val isConnected: Boolean get() = connected

    override fun transceive(data: ByteArray): ByteArray = pn533.inDataExchange(tg, data)

    override val maxTransceiveLength: Int get() = 261
}
