/*
 * UnknownVicinityTransitFactory.kt
 *
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
import com.codebutler.farebot.card.vicinity.VicinityCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import farebot.transit.vicinity.generated.resources.*
import com.codebutler.farebot.base.util.FormattedString

/**
 * Catch-all for NFC-V cards that no other factory matched.
 * Should be registered last in the Vicinity factory list.
 */
class UnknownVicinityTransitFactory : TransitFactory<VicinityCard, UnknownVicinityTransitInfo> {
    override val allCards: List<CardInfo> = emptyList()

    override fun check(card: VicinityCard): Boolean = card.pages.isNotEmpty()

    override fun parseIdentity(card: VicinityCard): TransitIdentity {
        val name = FormattedString(Res.string.unknown_nfcv_card)
        return TransitIdentity.create(name, null)
    }

    override fun parseInfo(card: VicinityCard): UnknownVicinityTransitInfo = UnknownVicinityTransitInfo()
}

class UnknownVicinityTransitInfo : TransitInfo() {
    override val cardName: FormattedString = FormattedString(Res.string.unknown_nfcv_card)

    override val serialNumber: String? = null

    override val info: List<ListItemInterface>
        get() =
            listOf(
                HeaderListItem(Res.string.unknown_card_title),
                ListItem(Res.string.unknown_card_desc),
            )
}
