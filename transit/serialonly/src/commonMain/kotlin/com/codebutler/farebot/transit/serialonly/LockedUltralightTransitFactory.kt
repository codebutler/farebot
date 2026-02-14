/*
 * LockedUltralightTransitFactory.kt
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import farebot.transit.serialonly.generated.resources.*

/**
 * Handle MIFARE Ultralight with no open pages (catch-all for unrecognized cards).
 * This should be the last executed MIFARE Ultralight check, after all the other checks are done.
 * This is because it will catch others' cards.
 */
class LockedUltralightTransitFactory : TransitFactory<UltralightCard, LockedUltralightTransitInfo> {
    override val allCards: List<CardInfo> = emptyList()

    /**
     * @param card Card to read.
     * @return true - this is the catch-all for Ultralight cards that weren't recognized.
     */
    override fun check(card: UltralightCard): Boolean {
        // If no other factory matched, treat it as an unknown/locked card.
        // Check that the card has pages beyond the header.
        return card.pages.size > 4
    }

    override fun parseIdentity(card: UltralightCard): TransitIdentity {
        val name = getStringBlocking(Res.string.locked_mfu_card)
        return TransitIdentity.create(name, null)
    }

    override fun parseInfo(card: UltralightCard): LockedUltralightTransitInfo = LockedUltralightTransitInfo()
}

class LockedUltralightTransitInfo : TransitInfo() {
    override val cardName: String = getStringBlocking(Res.string.locked_mfu_card)

    override val serialNumber: String? = null

    override val info: List<ListItemInterface>
        get() =
            listOf(
                HeaderListItem(Res.string.fully_locked_title),
                ListItem(Res.string.fully_locked_desc),
            )
}
