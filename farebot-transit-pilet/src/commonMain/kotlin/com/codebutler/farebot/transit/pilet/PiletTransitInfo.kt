/*
 * PiletTransitInfo.kt
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

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.readASCII
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.base.util.toHexDump
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_pilet.generated.resources.Res
import farebot.farebot_transit_pilet.generated.resources.pilet_card_effective_date
import farebot.farebot_transit_pilet.generated.resources.pilet_card_expiration_date
import farebot.farebot_transit_pilet.generated.resources.pilet_full_serial_number
import farebot.farebot_transit_pilet.generated.resources.pilet_interchange_control
import farebot.farebot_transit_pilet.generated.resources.pilet_issuer_country
import farebot.farebot_transit_pilet.generated.resources.pilet_kiev_digital_uid
import org.jetbrains.compose.resources.StringResource

/**
 * Transit data type for Pilet-based cards (Tartu Bus, Kyiv Digital).
 *
 * These are serial-only cards backed by pilet.ee NDEF records on MIFARE Classic.
 * Card number is extracted from the NDEF TLV payload on the card.
 *
 * Ported from Metrodroid's PiletTransitData, which displays extra EMV BER-TLV
 * fields from the card's NDEF payload via a TAG_MAP.
 */
class PiletTransitInfo(
    private val serial: String?,
    override val cardName: String,
    private val berTlvData: ByteArray?,
) : TransitInfo() {
    override val serialNumber: String?
        get() = serial

    override val info: List<ListItemInterface>?
        get() {
            val data = berTlvData ?: return null
            val items = parseBerTlvFields(data)
            if (items.isEmpty()) return null
            return items
        }

    companion object {
        // EMV tag constants matching Metrodroid's EmvData
        private const val TAG_CARD_EXPIRATION_DATE = 0x59
        private const val TAG_PAN = 0x5A
        private const val TAG_KYIV_DIGITAL_UID = 0x53
        private const val TAG_CARD_EFFECTIVE = 0x5F26
        private const val TAG_INTERCHANGE_PROTOCOL = 0x5F27
        private const val TAG_ISSUER_COUNTRY = 0x5F28

        /**
         * Tag map matching Metrodroid's PiletTransitData.TAG_MAP.
         * Maps BER-TLV tag numbers to (string resource, content type).
         */
        private val TAG_MAP: Map<Int, Pair<StringResource, TagContents>> =
            mapOf(
                TAG_PAN to Pair(Res.string.pilet_full_serial_number, TagContents.ASCII),
                TAG_ISSUER_COUNTRY to Pair(Res.string.pilet_issuer_country, TagContents.ASCII_NUM_COUNTRY),
                TAG_CARD_EXPIRATION_DATE to Pair(Res.string.pilet_card_expiration_date, TagContents.ASCII),
                TAG_CARD_EFFECTIVE to Pair(Res.string.pilet_card_effective_date, TagContents.ASCII),
                TAG_INTERCHANGE_PROTOCOL to Pair(Res.string.pilet_interchange_control, TagContents.ASCII),
                TAG_KYIV_DIGITAL_UID to Pair(Res.string.pilet_kiev_digital_uid, TagContents.DUMP_SHORT),
            )

        /**
         * Simple BER-TLV parser that extracts known tags and returns ListItems.
         *
         * Handles both single-byte tags (e.g. 0x53, 0x59, 0x5A) and multi-byte tags
         * (e.g. 0x5F26, 0x5F27, 0x5F28) per ISO 7816 BER-TLV encoding rules.
         */
        private fun parseBerTlvFields(data: ByteArray): List<ListItemInterface> {
            val results = mutableListOf<ListItemInterface>()
            var i = 0

            while (i < data.size) {
                // Parse tag
                val firstByte = data[i].toInt() and 0xFF
                i++

                // If lower 5 bits are all 1s, tag continues in subsequent bytes
                var tag = firstByte
                if (firstByte and 0x1F == 0x1F) {
                    while (i < data.size) {
                        val nextByte = data[i].toInt() and 0xFF
                        i++
                        tag = (tag shl 8) or nextByte
                        // If bit 7 is not set, this is the last tag byte
                        if (nextByte and 0x80 == 0) break
                    }
                }

                // Parse length
                if (i >= data.size) break
                var length = data[i].toInt() and 0xFF
                i++

                if (length and 0x80 != 0) {
                    val numLenBytes = length and 0x7F
                    if (numLenBytes == 0 || numLenBytes > 4 || i + numLenBytes > data.size) break
                    length = 0
                    for (j in 0 until numLenBytes) {
                        length = (length shl 8) or (data[i].toInt() and 0xFF)
                        i++
                    }
                }

                // Extract value
                if (i + length > data.size) break
                val value = data.sliceOffLen(i, length)
                i += length

                // Look up tag in our map
                val desc = TAG_MAP[tag] ?: continue
                val (labelRes, contents) = desc

                val displayValue =
                    when (contents) {
                        TagContents.ASCII -> value.readASCII()
                        TagContents.ASCII_NUM_COUNTRY -> value.readASCII()
                        TagContents.DUMP_SHORT -> value.toHexDump()
                    }

                if (displayValue.isNotEmpty()) {
                    results.add(ListItem(labelRes, displayValue))
                }
            }

            return results
        }
    }

    /**
     * Content type for BER-TLV tag values, matching Metrodroid's TagContents
     * for the subset used by Pilet cards.
     */
    private enum class TagContents {
        ASCII,
        ASCII_NUM_COUNTRY,
        DUMP_SHORT,
    }
}
