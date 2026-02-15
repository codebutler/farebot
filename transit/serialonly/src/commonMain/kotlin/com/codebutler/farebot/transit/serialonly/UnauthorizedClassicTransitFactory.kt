/*
 * UnauthorizedClassicTransitFactory.kt
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
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import farebot.transit.serialonly.generated.resources.*
import kotlinx.serialization.Serializable

/**
 * Catch-all for MIFARE Classic cards where all sectors are locked/unauthorized.
 * This factory should be registered LAST in the Classic factory list.
 */
class UnauthorizedClassicTransitFactory : TransitFactory<ClassicCard, UnauthorizedClassicTransitInfo> {
    override val allCards: List<CardInfo> = emptyList()

    /**
     * This should be the last executed MIFARE Classic check, after all the other checks are done.
     * This is because it will catch others' cards.
     *
     * @param card Card to read.
     * @return true if all sectors on the card are locked.
     */
    override fun check(card: ClassicCard): Boolean {
        // Check if ALL sectors are unauthorized (completely locked card)
        return card.sectors.all { it is UnauthorizedClassicSector }
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val name = FormattedString(Res.string.locked_mfc_card)
        return TransitIdentity.create(name, null)
    }

    override fun parseInfo(card: ClassicCard): UnauthorizedClassicTransitInfo {
        // Standard MIFARE Classic cards can be unlocked with keys
        // MIFARE Plus / DESFire emulation cannot be unlocked this way
        val isUnlockable = true // TODO: Detect MIFARE Plus when subType info is available
        return UnauthorizedClassicTransitInfo(isUnlockable = isUnlockable)
    }
}

@Serializable
data class UnauthorizedClassicTransitInfo(
    val isUnlockable: Boolean = false,
) : TransitInfo() {
    override val cardName: FormattedString get() = FormattedString(Res.string.locked_mfc_card)

    override val serialNumber: String? = null

    override val info: List<ListItemInterface>
        get() =
            listOf(
                HeaderListItem(Res.string.fully_locked_title),
                ListItem(
                    if (isUnlockable) {
                        Res.string.fully_locked_desc_unlockable
                    } else {
                        Res.string.fully_locked_desc
                    },
                ),
            )
}
