/*
 * FeliCaReader.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.card.felica

import com.codebutler.farebot.card.felica.raw.RawFelicaCard
import kotlin.time.Clock

/**
 * Shared FeliCa card-reading algorithm.
 *
 * Uses a [FeliCaTagAdapter] to communicate with the tag, producing
 * a [RawFelicaCard] with all discovered systems, services, and blocks.
 *
 * @param tagId The tag identifier
 * @param adapter The adapter for communicating with the tag
 * @param onlyFirst If `true`, only read the first system code on the card. If not set
 * (`false`), read all system codes. Setting this to `true` will result in an incomplete
 * read, but is needed to work around a bug in iOS.
 */
object FeliCaReader {
    suspend fun readTag(
        tagId: ByteArray,
        adapter: FeliCaTagAdapter,
        onlyFirst: Boolean = false,
        onProgress: (suspend (current: Int, total: Int) -> Unit)? = null,
    ): RawFelicaCard {
        val idmBytes = adapter.getIDm()
        val idm = FeliCaIdm(idmBytes)

        val systemCodes = adapter.getSystemCodes().toMutableList()

        var octopusMagic = false
        var sztMagic = false
        var liteMagic = false
        var isPartialRead = false

        // If no system codes reported, try FeliCa Lite, then Octopus/SZT magic
        if (systemCodes.isEmpty()) {
            // FeliCa Lite has no system code list
            if (adapter.selectSystem(FeliCaConstants.SYSTEMCODE_FELICA_LITE) != null) {
                systemCodes.add(FeliCaConstants.SYSTEMCODE_FELICA_LITE)
                liteMagic = true
            } else {
                // Don't try these on lite as it may respond to any code
                if (adapter.selectSystem(FeliCaConstants.SYSTEMCODE_OCTOPUS) != null) {
                    systemCodes.add(FeliCaConstants.SYSTEMCODE_OCTOPUS)
                    octopusMagic = true
                }
                if (adapter.selectSystem(FeliCaConstants.SYSTEMCODE_SZT) != null) {
                    systemCodes.add(FeliCaConstants.SYSTEMCODE_SZT)
                    sztMagic = true
                }
            }
        }

        // Get PMm from the first system (or use the initial IDm poll's PMm)
        val firstCode = systemCodes.firstOrNull() ?: FeliCaConstants.SYSTEMCODE_ANY
        val pmmBytes =
            adapter.selectSystem(firstCode)
                ?: throw Exception("Failed to poll for PMm")
        val pmm = FeliCaPmm(pmmBytes)

        val systems = mutableListOf<FelicaSystem>()

        for ((systemNumber, systemCode) in systemCodes.withIndex()) {
            onProgress?.invoke(systemNumber, systemCodes.size)
            // We can get System Code 0 from magic fallbacks -- drop this.
            if (systemCode == 0) continue

            if (onlyFirst && systemNumber > 0) {
                // We aren't going to read secondary system codes. Instead, insert a dummy
                // service with no service codes.
                // This is an iOS-specific hack to work around CoreNFC bug where
                // _NFReaderSession._validateFelicaCommand asserts that you're talking to the exact
                // IDm that the system discovered -- including the upper 4 bits (which indicate the
                // system number).
                systems.add(FelicaSystem.skipped(systemCode))
                continue
            }

            // Select (poll) this system
            adapter.selectSystem(systemCode)

            var serviceCodes: List<Int> =
                when {
                    octopusMagic && systemCode == FeliCaConstants.SYSTEMCODE_OCTOPUS ->
                        listOf(FeliCaConstants.SERVICE_OCTOPUS)
                    sztMagic && systemCode == FeliCaConstants.SYSTEMCODE_SZT ->
                        listOf(FeliCaConstants.SERVICE_SZT)
                    liteMagic && systemCode == FeliCaConstants.SYSTEMCODE_FELICA_LITE ->
                        listOf(FeliCaConstants.SERVICE_FELICA_LITE_READONLY)
                    else ->
                        adapter.getServiceCodes()
                }

            // Exclude services that require authentication (bit 0 == 0)
            val excludedCodes = serviceCodes.filter { it and 0x01 == 0 }
            serviceCodes = serviceCodes.filter { it and 0x01 == 1 }

            val services = mutableListOf<FelicaService>()

            // Mark excluded codes as skipped
            for (serviceCode in excludedCodes) {
                services.add(FelicaService.skipped(serviceCode))
            }

            for (serviceCode in serviceCodes) {
                // Re-select system before reading each service (matches Android behavior)
                adapter.selectSystem(systemCode)

                val blocks = mutableListOf<FelicaBlock>()

                // Read regular blocks
                var addr: Byte = 0
                while (true) {
                    val blockData = adapter.readBlock(serviceCode, addr)
                    if (blockData == null) {
                        // Card lost or read failure
                        if (blocks.isNotEmpty()) {
                            isPartialRead = true
                        }
                        break
                    }
                    blocks.add(FelicaBlock.create(addr, blockData))
                    addr++
                    if (addr >= 0x20 && liteMagic) break // Lite only has 0x20 regular blocks
                    if (addr < 0) break // overflow protection
                }

                // For FeliCa Lite, read extra block addresses
                if (systemCode == FeliCaConstants.SYSTEMCODE_FELICA_LITE) {
                    val extraAddrs = (0x80..0x88) + listOf(0x90, 0x92, 0xa0)
                    for (extraAddr in extraAddrs) {
                        val blockData = adapter.readBlock(serviceCode, extraAddr.toByte())
                        if (blockData == null) {
                            isPartialRead = true
                            break
                        }
                        blocks.add(FelicaBlock.create(extraAddr.toByte(), blockData))
                    }
                }

                if (blocks.isNotEmpty()) {
                    services.add(FelicaService.create(serviceCode, blocks))
                }
            }

            systems.add(FelicaSystem.create(systemCode, services, (serviceCodes + excludedCodes).toSet()))
        }

        return RawFelicaCard.create(tagId, Clock.System.now(), idm, pmm, systems, isPartialRead)
    }
}
