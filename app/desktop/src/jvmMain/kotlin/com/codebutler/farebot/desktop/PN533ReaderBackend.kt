/*
 * PN533ReaderBackend.kt
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

package com.codebutler.farebot.desktop

import com.codebutler.farebot.card.nfc.pn533.PN533
import com.codebutler.farebot.card.nfc.pn533.Usb4JavaPN533Transport

/**
 * NXP PN533 reader backend (e.g., SCM SCL3711).
 */
class PN533ReaderBackend(
    transport: Usb4JavaPN533Transport? = null,
) : PN53xReaderBackend(transport) {
    override val name: String = "PN533"

    override suspend fun initDevice(pn533: PN533) {
        val fw = pn533.getFirmwareVersion()
        println("[$name] Firmware: $fw")
        pn533.samConfiguration()
        pn533.setMaxRetries(passiveActivation = 0x02)
    }
}
