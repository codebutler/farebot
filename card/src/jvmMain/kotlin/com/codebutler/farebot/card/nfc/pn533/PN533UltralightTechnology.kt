/*
 * PN533UltralightTechnology.kt
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

import com.codebutler.farebot.card.nfc.UltralightTechnology

/**
 * PN533 implementation of [UltralightTechnology] for MIFARE Ultralight cards.
 *
 * Uses MIFARE Ultralight READ command (0x30) via [PN533.inDataExchange],
 * which returns 4 pages (16 bytes) starting from the given page offset.
 */
class PN533UltralightTechnology(
    private val pn533: PN533,
    private val tg: Int,
    private val info: PN533CardInfo,
) : UltralightTechnology {
    private var connected = true

    override fun connect() {
        connected = true
    }

    override fun close() {
        connected = false
    }

    override val isConnected: Boolean get() = connected

    override val type: Int get() = info.ultralightType

    override fun readPages(pageOffset: Int): ByteArray =
        pn533.inDataExchange(
            tg,
            byteArrayOf(MIFARE_CMD_READ, pageOffset.toByte()),
        )

    override fun transceive(data: ByteArray): ByteArray =
        pn533.inDataExchange(tg, data)

    companion object {
        const val MIFARE_CMD_READ: Byte = 0x30
    }
}
