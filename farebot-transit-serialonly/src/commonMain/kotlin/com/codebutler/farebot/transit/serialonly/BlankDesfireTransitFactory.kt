/*
 * BlankDesfireTransitFactory.kt
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
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_serialonly.generated.resources.*

/**
 * Detects blank MIFARE DESFire cards with no applications.
 * This factory should be registered near the END of the DESFire factory list,
 * just before UnauthorizedDesfireTransitFactory.
 */
class BlankDesfireTransitFactory : TransitFactory<DesfireCard, BlankDesfireTransitInfo> {
    override val allCards: List<CardInfo> = emptyList()

    /**
     * @param card Card to read.
     * @return true if the card has no applications (blank DESFire).
     */
    override fun check(card: DesfireCard): Boolean {
        // A blank DESFire card has no applications
        return card.applications.isEmpty()
    }

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        val name = getStringBlocking(Res.string.blank_mfd_card)
        return TransitIdentity.create(name, null)
    }

    override fun parseInfo(card: DesfireCard): BlankDesfireTransitInfo = BlankDesfireTransitInfo()
}

class BlankDesfireTransitInfo : TransitInfo() {
    override val cardName: String = getStringBlocking(Res.string.blank_mfd_card)

    override val serialNumber: String? = null

    override val info: List<ListItemInterface>
        get() =
            listOf(
                HeaderListItem(Res.string.fully_blank_title),
                ListItem(Res.string.fully_blank_desc),
            )
}
