/*
 * OctopusTransitFactory.kt
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Portions based on FelicaCard.java from nfcard project
 * Copyright 2013 Sinpo Wei <sinpowei@gmail.com>
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

package com.codebutler.farebot.transit.octopus

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.felica.FelicaCard
import com.codebutler.farebot.card.felica.FeliCaConstants
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_octopus.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class OctopusTransitFactory : TransitFactory<FelicaCard, OctopusTransitInfo> {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    override fun check(card: FelicaCard): Boolean {
        return (card.getSystem(FeliCaConstants.SYSTEMCODE_OCTOPUS) != null)
                || (card.getSystem(FeliCaConstants.SYSTEMCODE_SZT) != null)
    }

    override fun parseIdentity(card: FelicaCard): TransitIdentity = runBlocking {
        if (card.getSystem(FeliCaConstants.SYSTEMCODE_SZT) != null) {
            if (card.getSystem(FeliCaConstants.SYSTEMCODE_OCTOPUS) != null) {
                // Dual-mode card.
                TransitIdentity.create(getString(Res.string.octopus_dual_card_name), null)
            } else {
                // SZT-only card.
                TransitIdentity.create(getString(Res.string.octopus_szt_card_name), null)
            }
        } else {
            // Octopus-only card.
            TransitIdentity.create(getString(Res.string.octopus_card_name), null)
        }
    }

    override fun parseInfo(card: FelicaCard): OctopusTransitInfo {
        var octopusBalance: Int? = null
        var shenzhenBalance: Int? = null

        val octopusSystem = card.getSystem(FeliCaConstants.SYSTEMCODE_OCTOPUS)
        if (octopusSystem != null) {
            val service = octopusSystem.getService(FeliCaConstants.SERVICE_OCTOPUS)
            if (service != null) {
                val metadata = service.blocks[0].data
                val rawBalance = ByteUtils.byteArrayToInt(metadata, 0, 4)
                // Apply date-dependent offset and convert from 10-cent units to cents
                octopusBalance = (rawBalance - OctopusData.getOctopusOffset(card.scannedAt)) * 10
            }
        }

        val sztSystem = card.getSystem(FeliCaConstants.SYSTEMCODE_SZT)
        if (sztSystem != null) {
            val service = sztSystem.getService(FeliCaConstants.SERVICE_SZT)
            if (service != null) {
                val metadata = service.blocks[0].data
                val rawBalance = ByteUtils.byteArrayToInt(metadata, 0, 4)
                // Apply offset and convert from 10-cent units to cents
                shenzhenBalance = (rawBalance - OctopusData.getShenzhenOffset(card.scannedAt)) * 10
            }
        }

        return OctopusTransitInfo.create(
            octopusBalance,
            shenzhenBalance,
            hasOctopus = octopusBalance != null,
            hasShenzen = shenzhenBalance != null
        )
    }

    companion object {
        private val CARD_INFO = CardInfo(
            name = runBlocking { getString(Res.string.octopus_card_name) },
            cardType = CardType.FeliCa,
            region = TransitRegion.HONG_KONG,
            locationId = Res.string.location_hong_kong
        )
    }
}
