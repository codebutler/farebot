/*
 * IosCardTransceiver.kt
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
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

/**
 * iOS implementation of [CardTransceiver] wrapping Core NFC's [NFCMiFareTag].
 *
 * DESFire and CEPAS cards appear as MIFARE tags on iOS. The [NFCMiFareTag] protocol
 * supports sending raw APDU commands via [sendMiFareCommand], which is what
 * [DesfireProtocol] and [CEPASProtocol] use through [transceive].
 *
 * Core NFC APIs are asynchronous (completion handler based). This wrapper bridges
 * them to the synchronous [CardTransceiver] interface using dispatch semaphores,
 * which is safe because tag reading runs on a background thread.
 */
@OptIn(ExperimentalForeignApi::class)
class IosCardTransceiver(
    private val tag: NFCMiFareTagProtocol,
) : CardTransceiver {
    private var _isConnected = false

    override fun connect() {
        // Core NFC manages the connection lifecycle via NFCTagReaderSession.
        // When the session discovers a tag and connects to it, the tag is ready.
        _isConnected = true
    }

    override fun close() {
        _isConnected = false
    }

    override val isConnected: Boolean
        get() = _isConnected

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
            throw Exception("NFC transceive failed: ${it.localizedDescription}")
        }

        return result?.toByteArray()
            ?: throw Exception("NFC transceive returned null response")
    }

    override val maxTransceiveLength: Int
        get() = 253 // ISO 7816 APDU maximum command length
}
