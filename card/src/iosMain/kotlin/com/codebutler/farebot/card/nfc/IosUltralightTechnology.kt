/*
 * IosUltralightTechnology.kt
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

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreNFC.NFCMiFareTagProtocol
import platform.CoreNFC.NFCMiFareUltralight
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

/**
 * iOS implementation of [UltralightTechnology] wrapping Core NFC's [NFCMiFareTag].
 *
 * MIFARE Ultralight cards appear as [NFCMiFareTag] with [mifareFamily] == [NFCMiFareUltralight].
 * Read operations use the standard MIFARE Ultralight READ command (0x30) sent via
 * [sendMiFareCommand], which returns 16 bytes (4 pages of 4 bytes each).
 */
@OptIn(ExperimentalForeignApi::class)
class IosUltralightTechnology(
    private val tag: NFCMiFareTagProtocol,
) : UltralightTechnology {
    private var _isConnected = false

    override fun connect() {
        _isConnected = true
    }

    override fun close() {
        _isConnected = false
    }

    override val isConnected: Boolean
        get() = _isConnected

    override val type: Int
        get() =
            when (tag.mifareFamily) {
                NFCMiFareUltralight -> UltralightTechnology.TYPE_ULTRALIGHT
                else -> UltralightTechnology.TYPE_ULTRALIGHT
            }

    override fun readPages(pageOffset: Int): ByteArray {
        // MIFARE Ultralight READ command: 0x30 followed by the page number.
        // Returns 16 bytes (4 consecutive pages of 4 bytes each).
        val readCommand = byteArrayOf(0x30, pageOffset.toByte())

        val semaphore = dispatch_semaphore_create(0)
        var result: NSData? = null
        var nfcError: NSError? = null

        tag.sendMiFareCommand(readCommand.toNSData()) { response: NSData?, error: NSError? ->
            result = response
            nfcError = error
            dispatch_semaphore_signal(semaphore)
        }

        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)

        nfcError?.let {
            throw Exception("Ultralight read failed at page $pageOffset: ${it.localizedDescription}")
        }

        return result?.toByteArray()
            ?: throw Exception("Ultralight read returned null at page $pageOffset")
    }

    override fun transceive(data: ByteArray): ByteArray {
        val semaphore = dispatch_semaphore_create(0)
        var result: NSData? = null
        var nfcError: NSError? = null

        tag.sendMiFareCommand(data.toNSData()) { response: NSData?, error: NSError? ->
            result = response
            nfcError = error
            dispatch_semaphore_signal(semaphore)
        }

        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)

        nfcError?.let {
            throw Exception("Ultralight transceive failed: ${it.localizedDescription}")
        }

        return result?.toByteArray()
            ?: throw Exception("Ultralight transceive returned null")
    }
}
