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
 */
object FeliCaReader {
    fun readTag(
        tagId: ByteArray,
        adapter: FeliCaTagAdapter,
    ): RawFelicaCard {
        val idmBytes = adapter.getIDm()
        val idm = FeliCaIdm(idmBytes)

        val systemCodes = adapter.getSystemCodes().toMutableList()

        var octopusMagic = false
        var sztMagic = false

        // If no system codes reported, try Octopus/SZT magic
        if (systemCodes.isEmpty()) {
            if (adapter.selectSystem(FeliCaConstants.SYSTEMCODE_OCTOPUS) != null) {
                systemCodes.add(FeliCaConstants.SYSTEMCODE_OCTOPUS)
                octopusMagic = true
            }
            if (adapter.selectSystem(FeliCaConstants.SYSTEMCODE_SZT) != null) {
                systemCodes.add(FeliCaConstants.SYSTEMCODE_SZT)
                sztMagic = true
            }
        }

        // Get PMm from the first system (or use the initial IDm poll's PMm)
        val firstCode = systemCodes.firstOrNull() ?: FeliCaConstants.SYSTEMCODE_ANY
        val pmmBytes =
            adapter.selectSystem(firstCode)
                ?: throw Exception("Failed to poll for PMm")
        val pmm = FeliCaPmm(pmmBytes)

        val systems = mutableListOf<FelicaSystem>()

        for (systemCode in systemCodes) {
            // Select (poll) this system
            adapter.selectSystem(systemCode)

            val serviceCodes: List<Int> =
                when {
                    octopusMagic && systemCode == FeliCaConstants.SYSTEMCODE_OCTOPUS ->
                        listOf(FeliCaConstants.SERVICE_OCTOPUS)
                    sztMagic && systemCode == FeliCaConstants.SYSTEMCODE_SZT ->
                        listOf(FeliCaConstants.SERVICE_SZT)
                    else ->
                        adapter.getServiceCodes()
                }

            val services = mutableListOf<FelicaService>()
            for (serviceCode in serviceCodes) {
                // Re-select system before reading each service (matches Android behavior)
                adapter.selectSystem(systemCode)

                val blocks = mutableListOf<FelicaBlock>()
                var addr: Byte = 0
                while (true) {
                    val blockData = adapter.readBlock(serviceCode, addr) ?: break
                    blocks.add(FelicaBlock.create(addr, blockData))
                    addr++
                    if (addr < 0) break // overflow protection
                }

                if (blocks.isNotEmpty()) {
                    services.add(FelicaService.create(serviceCode, blocks))
                }
            }

            systems.add(FelicaSystem.create(systemCode, services, serviceCodes.toSet()))
        }

        return RawFelicaCard.create(tagId, Clock.System.now(), idm, pmm, systems)
    }
}
