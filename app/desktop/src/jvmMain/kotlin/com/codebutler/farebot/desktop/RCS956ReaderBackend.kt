/*
 * RCS956ReaderBackend.kt
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
import com.codebutler.farebot.card.nfc.pn533.PN533Transport

/**
 * Sony RC-S956 reader backend (RC-S370/P, RC-S380).
 *
 * The RC-S956 shares the PN53x command set but has no SAM module
 * and requires a different initialization sequence. This follows
 * nfcpy rcs956.py Device.__init__() exactly.
 *
 * Reference: https://github.com/nfcpy/nfcpy/blob/master/src/nfc/clf/rcs956.py
 */
class RCS956ReaderBackend(
    transport: PN533Transport,
    private val deviceLabel: String = "RC-S956",
) : PN53xReaderBackend(transport) {
    override val name: String = deviceLabel

    override fun initDevice(pn533: PN533) {
        // nfcpy rcs956.py init(transport) + Device.__init__() sequence.
        //
        // 1. ACK clears device state after USB connect (init function)
        // 2. resetMode #1 switches to Mode 0 (Device.__init__)
        // 3. getFirmwareVersion identifies device
        // 4. mute() = resetMode #2 + rfConfig auto-RFCA (Device.__init__)
        // 5. RF configuration
        // 6. resetMode #3 after config (Device.__init__)
        // 7. writeRegister CIU setup

        pn533.sendAck() // nfcpy init(transport): transport.write(Chipset.ACK)
        pn533.resetMode()

        val fw = pn533.getFirmwareVersion()
        println("[$name] Firmware: $fw (RC-S956)")

        // mute() = resetMode + super().mute()
        pn533.resetMode()
        pn533.rfConfiguration(0x01, byteArrayOf(0x02)) // super().mute(): auto RFCA

        pn533.rfConfiguration(0x02, byteArrayOf(0x0B, 0x0B, 0x0A)) // timings
        pn533.rfConfiguration(0x04, byteArrayOf(0x00)) // MaxRtyCOM
        pn533.rfConfiguration(0x05, byteArrayOf(0x00, 0x00, 0x01)) // MaxRetries
        pn533.rfConfiguration(
            0x0A,
            byteArrayOf(
                0x5A, 0xF4.toByte(), 0x3F, 0x11, 0x4D,
                0x85.toByte(), 0x61, 0x6F, 0x26, 0x62, 0x87.toByte(),
            ),
        ) // 106kbps Type A
        pn533.setParameters(0x08)
        pn533.resetMode()
        pn533.writeRegister(0x0328, 0x59)
    }
}
