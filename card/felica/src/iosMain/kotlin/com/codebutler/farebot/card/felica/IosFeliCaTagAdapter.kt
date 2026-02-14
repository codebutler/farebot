/*
 * IosFeliCaTagAdapter.kt
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

package com.codebutler.farebot.card.felica

import com.codebutler.farebot.card.nfc.toByteArray
import com.codebutler.farebot.card.nfc.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreNFC.NFCFeliCaPollingRequestCodeNoRequest
import platform.CoreNFC.NFCFeliCaPollingTimeSlotMax1
import platform.CoreNFC.NFCFeliCaTagProtocol
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

/**
 * iOS implementation of [FeliCaTagAdapter] using Core NFC's [NFCFeliCaTagProtocol].
 *
 * Uses semaphore-based bridging for the async Core NFC API.
 */
@OptIn(ExperimentalForeignApi::class)
class IosFeliCaTagAdapter(
    private val tag: NFCFeliCaTagProtocol,
) : FeliCaTagAdapter {
    override fun getIDm(): ByteArray = tag.currentIDm.toByteArray()

    override fun getSystemCodes(): List<Int> {
        val semaphore = dispatch_semaphore_create(0)
        var codes: List<*>? = null
        var nfcError: NSError? = null

        tag.requestSystemCodeWithCompletionHandler { systemCodes: List<*>?, error: NSError? ->
            codes = systemCodes
            nfcError = error
            dispatch_semaphore_signal(semaphore)
        }

        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)

        if (nfcError != null) return emptyList()

        return codes?.mapNotNull { item ->
            val data = item as? NSData ?: return@mapNotNull null
            val bytes = data.toByteArray()
            if (bytes.size >= 2) {
                ((bytes[0].toInt() and 0xff) shl 8) or (bytes[1].toInt() and 0xff)
            } else {
                null
            }
        } ?: emptyList()
    }

    override fun selectSystem(systemCode: Int): ByteArray? {
        val semaphore = dispatch_semaphore_create(0)
        var pmmData: NSData? = null
        var nfcError: NSError? = null

        val systemCodeBytes =
            byteArrayOf(
                (systemCode shr 8).toByte(),
                (systemCode and 0xff).toByte(),
            )

        tag.pollingWithSystemCode(
            systemCodeBytes.toNSData(),
            requestCode = NFCFeliCaPollingRequestCodeNoRequest,
            timeSlot = NFCFeliCaPollingTimeSlotMax1,
        ) { pmm: NSData?, _: NSData?, error: NSError? ->
            pmmData = pmm
            nfcError = error
            dispatch_semaphore_signal(semaphore)
        }

        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)

        if (nfcError != null) return null
        return pmmData?.toByteArray()
    }

    override fun getServiceCodes(): List<Int> {
        val candidates = mutableListOf<Int>()
        for (n in 0 until MAX_SERVICE_NUMBER) {
            for (attr in PROBE_ATTRIBUTES) {
                candidates.add((n shl 6) or attr)
            }
        }

        val discovered = mutableListOf<Int>()

        // Probe in batches of 32 (FeliCa REQUEST_SERVICE limit)
        for (batch in candidates.chunked(32)) {
            val versions = requestServiceVersions(batch) ?: continue
            for (i in versions.indices) {
                if (i < batch.size && versions[i] != 0xFFFF) {
                    discovered.add(batch[i])
                }
            }
        }

        return discovered
    }

    override fun readBlock(
        serviceCode: Int,
        blockAddr: Byte,
    ): ByteArray? {
        val semaphore = dispatch_semaphore_create(0)
        var blockDataList: List<*>? = null
        var nfcError: NSError? = null

        // Service code list: 2 bytes, little-endian
        val serviceCodeData =
            byteArrayOf(
                (serviceCode and 0xff).toByte(),
                (serviceCode shr 8).toByte(),
            ).toNSData()

        // Block list element: 2-byte format (0x80 | service_list_order, block_number)
        val blockListData = byteArrayOf(0x80.toByte(), blockAddr).toNSData()

        tag.readWithoutEncryptionWithServiceCodeList(
            listOf(serviceCodeData),
            blockList = listOf(blockListData),
        ) { _: Long, _: Long, dataList: List<*>?, error: NSError? ->
            blockDataList = dataList
            nfcError = error
            dispatch_semaphore_signal(semaphore)
        }

        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)

        if (nfcError != null) return null

        val data = blockDataList?.firstOrNull() as? NSData ?: return null
        val bytes = data.toByteArray()
        return if (bytes.isNotEmpty()) bytes else null
    }

    private fun requestServiceVersions(serviceCodes: List<Int>): List<Int>? {
        val semaphore = dispatch_semaphore_create(0)
        var versionList: List<*>? = null
        var nfcError: NSError? = null

        val nodeCodeList =
            serviceCodes.map { code ->
                byteArrayOf(
                    (code and 0xff).toByte(),
                    (code shr 8).toByte(),
                ).toNSData()
            }

        tag.requestServiceWithNodeCodeList(nodeCodeList) { versions: List<*>?, error: NSError? ->
            versionList = versions
            nfcError = error
            dispatch_semaphore_signal(semaphore)
        }

        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)

        if (nfcError != null) return null

        return versionList?.map { item ->
            val data = item as? NSData ?: return@map 0xFFFF
            val bytes = data.toByteArray()
            if (bytes.size >= 2) {
                (bytes[0].toInt() and 0xff) or ((bytes[1].toInt() and 0xff) shl 8)
            } else {
                0xFFFF
            }
        }
    }

    companion object {
        private const val MAX_SERVICE_NUMBER = 128

        // Service code attributes to probe for. Includes both read-only and read-write
        // attributes so that card type detection (which relies on identifying unique
        // service codes) works correctly even for services we can't read data from.
        private val PROBE_ATTRIBUTES =
            listOf(
                0x08,
                0x09,
                0x0A,
                0x0B, // Random: R/W key, R/O key, R/W no-key, R/O no-key
                0x0C,
                0x0D,
                0x0E,
                0x0F, // Cyclic: R/W key, R/O key, R/W no-key, R/O no-key
                0x17, // Purse: Cashback R/O no-key
            )
    }
}
