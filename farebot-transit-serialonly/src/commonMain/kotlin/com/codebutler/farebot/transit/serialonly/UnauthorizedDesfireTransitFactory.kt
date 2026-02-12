/*
 * UnauthorizedDesfireTransitFactory.kt
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
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.UnauthorizedDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_serialonly.generated.resources.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString

/**
 * Catch-all for MIFARE DESFire cards where all files are locked/unauthorized.
 * This factory should be registered LAST in the DESFire factory list.
 */
class UnauthorizedDesfireTransitFactory : TransitFactory<DesfireCard, UnauthorizedDesfireTransitInfo> {

    override val allCards: List<CardInfo> = emptyList()

    /**
     * This should be the last executed MIFARE DESFire check, after all the other checks are done.
     * This is because it will catch others' cards.
     *
     * @param card Card to read.
     * @return true if all files on the card are locked.
     */
    override fun check(card: DesfireCard): Boolean {
        // If there are no applications, this is a blank card (handled by BlankDesfireTransitFactory)
        if (card.applications.isEmpty()) return false

        // Check if ALL files across ALL applications are unauthorized
        return card.applications.all { app ->
            app.files.all { it is UnauthorizedDesfireFile }
        }
    }

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        val cardName = getName(card)
        return TransitIdentity.create(cardName, null)
    }

    override fun parseInfo(card: DesfireCard): UnauthorizedDesfireTransitInfo {
        val cardName = getName(card)
        return UnauthorizedDesfireTransitInfo(cardName = cardName)
    }

    companion object {
        /**
         * Known locked card types identified by their DESFire application ID.
         */
        private data class UnauthorizedType(
            val appId: Int,
            val name: String
        )

        private val TYPES = listOf(
            UnauthorizedType(0x31594f, "Oyster"),
            UnauthorizedType(0x425311, "Thailand BEM"),
            UnauthorizedType(0x425303, "Rabbit Card"),
            UnauthorizedType(0x5011f2, "Litacka"),
            UnauthorizedType(0x5010f2, "Metrocard (Christchurch)")
        )

        /**
         * Application ID range for hidden/reserved DESFire apps.
         * These are typically system applications that shouldn't be shown to users.
         */
        val HIDDEN_APP_IDS: List<Int> = List(32) { 0x425300 + it }

        private fun getName(card: DesfireCard): String {
            for ((appId, name) in TYPES) {
                if (card.getApplication(appId) != null) {
                    return name
                }
            }
            return runBlocking { getString(Res.string.locked_mfd_card) }
        }
    }
}

@Serializable
data class UnauthorizedDesfireTransitInfo(
    override val cardName: String
) : TransitInfo() {
    override val serialNumber: String? = null

    override val info: List<ListItemInterface>
        get() = listOf(
            HeaderListItem(Res.string.fully_locked_title),
            ListItem(Res.string.fully_locked_desc)
        )
}
