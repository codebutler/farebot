/*
 * BlankUltralightTransitFactory.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import farebot.transit.serialonly.generated.resources.*
import com.codebutler.farebot.base.util.FormattedString

/**
 * Handle MIFARE Ultralight with no non-default data.
 * This factory should be registered near the END of the Ultralight factory list.
 */
class BlankUltralightTransitFactory : TransitFactory<UltralightCard, BlankUltralightTransitInfo> {
    override val allCards: List<CardInfo> = emptyList()

    /**
     * @param card Card to read.
     * @return true if all sectors on the card are blank.
     */
    override fun check(card: UltralightCard): Boolean {
        val pages = card.pages
        val model = getCardModel(card)

        // Check to see if all sectors are blocked
        for ((idx, p) in pages.withIndex()) {
            // Page 2 is serial, internal and lock bytes
            // Page 3 is OTP counters
            // User memory is page 4 and above
            if (idx <= 2) {
                continue
            }
            val data = p.data

            // Check if this looks like an unauthorized/unreadable page (empty data)
            if (data.isEmpty()) {
                // At least one page is "closed", this is not for us
                return false
            }

            if (idx == 0x2) {
                if (data.size >= 4 && (data[2].toInt() != 0 || data[3].toInt() != 0)) {
                    return false
                }
                continue
            }

            if (model.startsWith("NTAG21")) {
                // Factory-set data on NTAG
                if (model == "NTAG213") {
                    if (idx == 0x03 &&
                        data.contentEquals(
                            byteArrayOf(0xE1.toByte(), 0x10, 0x12, 0),
                        )
                    ) {
                        continue
                    }
                    if (idx == 0x04 &&
                        data.contentEquals(
                            byteArrayOf(0x01, 0x03, 0xA0.toByte(), 0x0C),
                        )
                    ) {
                        continue
                    }
                    if (idx == 0x05 &&
                        data.contentEquals(
                            byteArrayOf(0x34, 0x03, 0, 0xFE.toByte()),
                        )
                    ) {
                        continue
                    }
                }

                if (model == "NTAG215") {
                    if (idx == 0x03 &&
                        data.contentEquals(
                            byteArrayOf(0xE1.toByte(), 0x10, 0x3E, 0),
                        )
                    ) {
                        continue
                    }
                    if (idx == 0x04 &&
                        data.contentEquals(
                            byteArrayOf(0x03, 0, 0xFE.toByte(), 0),
                        )
                    ) {
                        continue
                    }
                    // Page 5 is all null
                }

                if (model == "NTAG216") {
                    if (idx == 0x03 &&
                        data.contentEquals(
                            byteArrayOf(0xE1.toByte(), 0x10, 0x6D, 0),
                        )
                    ) {
                        continue
                    }
                    if (idx == 0x04 &&
                        data.contentEquals(
                            byteArrayOf(0x03, 0, 0xFE.toByte(), 0),
                        )
                    ) {
                        continue
                    }
                    // Page 5 is all null
                }

                // Ignore configuration pages
                if (idx == pages.size - 5) {
                    // LOCK BYTE / RFUI
                    // Only care about first three bytes
                    if (data.size >= 3 && data.copyOfRange(0, 3).contentEquals(byteArrayOf(0, 0, 0))) {
                        continue
                    }
                }

                if (idx == pages.size - 4) {
                    // MIRROR / RFUI / MIRROR_PAGE / AUTH0
                    // STRG_MOD_EN = 1
                    // AUTH0 = 0xff
                    if (data.contentEquals(byteArrayOf(4, 0, 0, 0xFF.toByte()))) {
                        continue
                    }
                }

                if (idx == pages.size - 3) {
                    // ACCESS / RFUI
                    // Only care about first byte
                    if (data.isNotEmpty() && data[0].toInt() == 0) {
                        continue
                    }
                }

                if (idx == pages.size - 2) {
                    // PWD (always masked)
                    // PACK / RFUI
                    continue
                }
            } else {
                // page 0x10 and 0x11 on 384-bit card are config
                if (pages.size == 0x14) {
                    if (idx == 0x10 && data.contentEquals(byteArrayOf(0, 0, 0, -1))) {
                        continue
                    }
                    if (idx == 0x11 && data.contentEquals(byteArrayOf(0, 5, 0, 0))) {
                        continue
                    }
                }
            }

            if (!data.contentEquals(byteArrayOf(0, 0, 0, 0))) {
                return false
            }
        }
        return true
    }

    override fun parseIdentity(card: UltralightCard): TransitIdentity {
        val name = FormattedString(Res.string.blank_mfu_card)
        return TransitIdentity.create(name, null)
    }

    override fun parseInfo(card: UltralightCard): BlankUltralightTransitInfo = BlankUltralightTransitInfo()

    /**
     * Get the card model name from the Ultralight card type.
     * Returns empty string if unknown.
     */
    private fun getCardModel(card: UltralightCard): String =
        when (card.ultralightType) {
            UltralightCard.UltralightType.NTAG213.ordinal -> "NTAG213"
            UltralightCard.UltralightType.NTAG215.ordinal -> "NTAG215"
            UltralightCard.UltralightType.NTAG216.ordinal -> "NTAG216"
            else -> ""
        }
}

class BlankUltralightTransitInfo : TransitInfo() {
    override val cardName: FormattedString = FormattedString(Res.string.blank_mfu_card)

    override val serialNumber: String? = null

    override val info: List<ListItemInterface>
        get() =
            listOf(
                HeaderListItem(Res.string.fully_blank_title),
                ListItem(Res.string.fully_blank_desc),
            )
}
