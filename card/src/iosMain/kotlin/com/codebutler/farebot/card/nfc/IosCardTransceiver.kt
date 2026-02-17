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
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreNFC.NFCMiFareTagProtocol
import platform.Foundation.NSData
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS implementation of [CardTransceiver] wrapping Core NFC's [NFCMiFareTag].
 *
 * DESFire and CEPAS cards appear as MIFARE tags on iOS. The [NFCMiFareTag] protocol
 * supports sending raw APDU commands via [sendMiFareCommand], which is what
 * [DesfireProtocol] and [CEPASProtocol] use through [transceive].
 *
 * Core NFC APIs are asynchronous (completion handler based). This wrapper bridges
 * them to the suspend [CardTransceiver] interface using [suspendCancellableCoroutine].
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

    override suspend fun transceive(data: ByteArray): ByteArray =
        suspendCancellableCoroutine { cont ->
            tag.sendMiFareCommand(data.toNSData()) { response: NSData?, error: NSError? ->
                if (error != null) {
                    cont.resumeWithException(Exception("NFC transceive failed: ${error.localizedDescription}"))
                } else if (response != null) {
                    cont.resume(response.toByteArray())
                } else {
                    cont.resumeWithException(Exception("NFC transceive returned null response"))
                }
            }
        }

    override val maxTransceiveLength: Int
        get() = 253 // ISO 7816 APDU maximum command length
}
