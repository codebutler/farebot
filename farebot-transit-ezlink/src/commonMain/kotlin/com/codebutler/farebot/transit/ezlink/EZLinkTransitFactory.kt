/*
 * EZLinkTransitFactory.kt
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
 * Copyright (C) 2012 tbonang <bonang@gmail.com>
 * Copyright (C) 2012 Victor Heng <bakavic@gmail.com>
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

package com.codebutler.farebot.transit.ezlink

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.cepas.CEPASCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_ezlink.generated.resources.*

class EZLinkTransitFactory(
    private val stringResource: StringResource,
) : TransitFactory<CEPASCard, EZLinkTransitInfo> {
    override val allCards: List<CardInfo>
        get() = ALL_CARDS

    override fun check(card: CEPASCard): Boolean = card.getPurse(3) != null

    override fun parseIdentity(card: CEPASCard): TransitIdentity {
        val purse =
            card.getPurse(3) ?: return TransitIdentity.create(
                EZLinkData.getCardIssuer(null, stringResource),
                null,
            )
        val canNo = purse.can!!.hex()
        return TransitIdentity.create(EZLinkData.getCardIssuer(canNo, stringResource), canNo)
    }

    override fun parseInfo(card: CEPASCard): EZLinkTransitInfo {
        val purse = card.getPurse(3)
        if (purse == null) {
            return EZLinkTransitInfo(
                serialNumber = null,
                mBalance = null,
                trips = parseTrips(card, EZLinkData.getCardIssuer(null, stringResource)),
                stringResource = stringResource,
            )
        }
        val canNo = purse.can!!.hex()
        return EZLinkTransitInfo(
            serialNumber = canNo,
            mBalance = purse.purseBalance,
            trips = parseTrips(card, EZLinkData.getCardIssuer(canNo, stringResource)),
            stringResource = stringResource,
        )
    }

    private fun parseTrips(
        card: CEPASCard,
        cardName: String,
    ): List<EZLinkTrip> {
        val history = card.getHistory(3) ?: return emptyList()
        val transactions = history.transactions ?: return emptyList()
        return transactions.map { EZLinkTrip(it, cardName, stringResource) }
    }

    companion object {
        private val ALL_CARDS =
            listOf(
                CardInfo(
                    nameRes = Res.string.card_name_ezlink,
                    cardType = CardType.CEPAS,
                    region = TransitRegion.SINGAPORE,
                    locationRes = Res.string.location_singapore,
                    imageRes = Res.drawable.ezlink_card,
                    latitude = 1.3521f,
                    longitude = 103.8198f,
                    brandColor = 0x0199D9,
                    credits = listOf("Sean Cross", "Victor Heng", "Toby Bonang"),
                    sampleDumpFile = "EZLink.json",
                    extraNoteRes = Res.string.ezlink_card_note,
                ),
                CardInfo(
                    nameRes = Res.string.card_name_nets,
                    cardType = CardType.CEPAS,
                    region = TransitRegion.SINGAPORE,
                    locationRes = Res.string.location_singapore,
                    imageRes = Res.drawable.nets_card,
                    latitude = 1.3521f,
                    longitude = 103.8198f,
                    brandColor = 0x003DA5,
                    credits = listOf("Sean Cross", "Victor Heng", "Toby Bonang"),
                    extraNoteRes = Res.string.ezlink_card_note,
                ),
            )
    }
}
