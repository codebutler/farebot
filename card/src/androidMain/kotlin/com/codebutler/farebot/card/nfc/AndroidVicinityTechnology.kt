/*
 * AndroidVicinityTechnology.kt
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

import android.nfc.tech.NfcV

/**
 * Android implementation of [VicinityTechnology] using Android's [NfcV] API.
 */
class AndroidVicinityTechnology(
    private val nfcV: NfcV,
) : VicinityTechnology {
    override fun connect() {
        nfcV.connect()
    }

    override fun close() {
        nfcV.close()
    }

    override val isConnected: Boolean
        get() = nfcV.isConnected

    override val uid: ByteArray
        get() = nfcV.tag.id

    override fun transceive(data: ByteArray): ByteArray = nfcV.transceive(data)
}
