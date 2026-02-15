/*
 * IosVicinityTechnology.kt
 *
 * Copyright 2019,2021 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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
import platform.CoreNFC.NFCISO15693TagProtocol
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

/**
 * iOS implementation of [VicinityTechnology] using Core NFC's [NFCISO15693TagProtocol].
 *
 * Uses semaphore-based bridging for the async Core NFC API.
 */
@OptIn(ExperimentalForeignApi::class)
class IosVicinityTechnology(
    private val tag: NFCISO15693TagProtocol,
) : VicinityTechnology {
    private var connected = false

    override fun connect() {
        connected = true
    }

    override fun close() {
        connected = false
    }

    override val isConnected: Boolean
        get() = connected

    override val uid: ByteArray
        get() = tag.identifier.toByteArray().reversedArray()

    /**
     * Read a single block using the readSingleBlock API.
     *
     * For Vicinity cards, we use the simpler readSingleBlock API instead of
     * sendCustomCommand, as it's more reliable across different card types.
     *
     * Error codes:
     * - 102: End of memory (no more blocks to read)
     * - 100: Tag lost
     */
    override fun transceive(data: ByteArray): ByteArray {
        // Parse the command to extract block number
        // Expected format: [flags, cmd, ...uid..., blockNumber]
        // For ISO 15693: flags=0x22, cmd=0x20 (read single block), uid=8 bytes, blockNum=1 byte
        if (data.size < 11 || data[1] != 0x20.toByte()) {
            throw Exception("Unsupported command for iOS NFC-V: ${data.contentToString()}")
        }

        val blockNumber = data[10].toUByte()

        val semaphore = dispatch_semaphore_create(0)
        var blockData: NSData? = null
        var nfcError: NSError? = null

        tag.readSingleBlockWithRequestFlags(
            0x22u,
            blockNumber = blockNumber,
        ) { data: NSData?, error: NSError? ->
            blockData = data
            nfcError = error
            dispatch_semaphore_signal(semaphore)
        }

        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)

        nfcError?.let { error ->
            when (error.code) {
                102L -> throw EndOfMemoryException()
                100L -> throw TagLostException()
                else -> throw Exception("NFC-V read error: ${error.localizedDescription}")
            }
        }

        val bytes = blockData?.toByteArray() ?: throw Exception("No data returned")
        // Prepend success status byte (0x00) to match Android NfcV.transceive behavior
        return byteArrayOf(0x00) + bytes
    }

    class EndOfMemoryException : Exception("End of memory")

    class TagLostException : Exception("Tag lost")
}
