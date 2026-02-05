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
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_vicinity.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Catch-all for NFC-V cards that no other factory matched.
 * Should be registered last in the Vicinity factory list.
 */
class UnknownVicinityTransitFactory : TransitFactory<VicinityCard, UnknownVicinityTransitInfo> {

    override fun check(card: VicinityCard): Boolean {
        return card.pages.isNotEmpty()
    }

    override fun parseIdentity(card: VicinityCard): TransitIdentity {
        val name = runBlocking { getString(Res.string.unknown_nfcv_card) }
        return TransitIdentity.create(name, null)
    }

    override fun parseInfo(card: VicinityCard): UnknownVicinityTransitInfo {
        return UnknownVicinityTransitInfo()
    }
}

class UnknownVicinityTransitInfo : TransitInfo() {
    override val cardName: String = runBlocking { getString(Res.string.unknown_nfcv_card) }

    override val serialNumber: String? = null

    override val info: List<ListItemInterface>
        get() = listOf(
            HeaderListItem(Res.string.unknown_card_title),
            ListItem(Res.string.unknown_card_desc)
        )
}
