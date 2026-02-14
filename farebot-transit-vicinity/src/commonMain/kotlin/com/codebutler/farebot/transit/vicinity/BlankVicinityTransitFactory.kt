/*
 * BlankVicinityTransitFactory.kt
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

package com.codebutler.farebot.transit.vicinity

import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.vicinity.VicinityCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_vicinity.generated.resources.*

/**
 * Handle NFC-V (ISO 15693) cards with no non-default data.
 * Detects blank NFC-V cards.
 */
class BlankVicinityTransitFactory : TransitFactory<VicinityCard, BlankVicinityTransitInfo> {
    override val allCards: List<CardInfo> = emptyList()

    /**
     * @param card Card to read.
     * @return true if all sectors on the card are blank.
     */
    override fun check(card: VicinityCard): Boolean {
        val pages = card.pages
        return pages.isNotEmpty() && pages.all { it.data.all { byte -> byte == 0.toByte() } }
    }

    override fun parseIdentity(card: VicinityCard): TransitIdentity {
        val name = getStringBlocking(Res.string.blank_nfcv_card)
        return TransitIdentity.create(name, null)
    }

    override fun parseInfo(card: VicinityCard): BlankVicinityTransitInfo = BlankVicinityTransitInfo()
}

class BlankVicinityTransitInfo : TransitInfo() {
    override val cardName: String = getStringBlocking(Res.string.blank_nfcv_card)

    override val serialNumber: String? = null

    override val info: List<ListItemInterface>
        get() =
            listOf(
                HeaderListItem(Res.string.fully_blank_title),
                ListItem(Res.string.fully_blank_desc),
            )
}
