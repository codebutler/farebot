/*
 * AndroidCardTransceiver.kt
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

import android.nfc.tech.IsoDep

class AndroidCardTransceiver(
    private val isoDep: IsoDep,
) : CardTransceiver {
    override fun connect() {
        isoDep.connect()
    }

    override fun close() {
        isoDep.close()
    }

    override val isConnected: Boolean
        get() = isoDep.isConnected

    override fun transceive(data: ByteArray): ByteArray = isoDep.transceive(data)

    override val maxTransceiveLength: Int
        get() = isoDep.maxTransceiveLength
}
