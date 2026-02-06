/*
 * TartuTransitFactory.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.pilet

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.readASCII
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import farebot.farebot_transit_pilet.generated.resources.Res
import farebot.farebot_transit_pilet.generated.resources.pilet_tartu_card_name
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Transit data type for Tartu bus card.
 *
 * This is a very limited implementation of reading TartuBus, because only
 * little data is stored on the card.
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/TartuBus
 */
class TartuTransitFactory : TransitFactory<ClassicCard, PiletTransitInfo> {

    companion object {
        private const val NDEF_TYPE = "pilet.ee:ekaart:2"
        private const val SERIAL_PREFIX_LEN = 8
    }

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false
        if (sector0.getBlock(1).data.byteArrayToInt(2, 4) != 0x03e103e1) return false

        val sector1 = card.getSector(1)
        if (sector1 !is DataClassicSector) return false
        if (!sector1.getBlock(0).data.sliceOffLen(7, 9)
                .contentEquals("pilet.ee:".encodeToByteArray())
        ) return false
        if (!sector1.getBlock(1).data.sliceOffLen(0, 8)
                .contentEquals("ekaart:2".encodeToByteArray())
        ) return false

        return true
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val serial = getSerial(card)
        return TransitIdentity.create(runBlocking { getString(Res.string.pilet_tartu_card_name) }, serial)
    }

    override fun parseInfo(card: ClassicCard): PiletTransitInfo {
        val ndefData = collectNdefData(card, startSector = 1)
        return PiletTransitInfo(
            serial = getSerialFromTlv(ndefData),
            cardName = runBlocking { getString(Res.string.pilet_tartu_card_name) },
            berTlvData = ndefData
        )
    }

    /**
     * Extracts the serial number from the NDEF TLV payload.
     * The payload contains a BER-TLV with PAN (tag 5A) containing the serial as ASCII.
     * We search for the PAN tag and strip the prefix.
     */
    private fun getSerial(card: ClassicCard): String? {
        val data = collectNdefData(card, startSector = 1) ?: return null
        return getSerialFromTlv(data)
    }

    private fun getSerialFromTlv(data: ByteArray?): String? {
        if (data == null) return null
        return findBerTlvAscii(data, 0x5A)?.let { pan ->
            if (pan.length > SERIAL_PREFIX_LEN) pan.substring(SERIAL_PREFIX_LEN) else pan
        }
    }

    /**
     * Collects all data blocks from NDEF sectors into a single byte array.
     */
    private fun collectNdefData(card: ClassicCard, startSector: Int): ByteArray? {
        return try {
            val allData = mutableListOf<Byte>()
            for (sectorIdx in startSector until card.sectors.size) {
                val sector = card.getSector(sectorIdx) as? DataClassicSector ?: continue
                for (blockIdx in 0 until sector.blocks.size) {
                    val block = sector.getBlock(blockIdx)
                    if (block.type == "data") {
                        allData.addAll(block.data.toList())
                    }
                }
            }
            if (allData.isEmpty()) null else allData.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Simple BER-TLV search: finds a single-byte tag and returns its value as ASCII string.
     */
    private fun findBerTlvAscii(data: ByteArray, tag: Int): String? {
        var i = 0
        while (i < data.size - 2) {
            val t = data[i].toInt() and 0xFF
            if (t == tag) {
                val len = data[i + 1].toInt() and 0xFF
                if (i + 2 + len <= data.size) {
                    return data.sliceOffLen(i + 2, len).readASCII()
                }
            }
            i++
        }
        return null
    }
}
