/*
 * TroikaHybridTransitFactory.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.troika

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.podorozhnik.PodorozhnikTransitFactory
import com.codebutler.farebot.transit.serialonly.StrelkaTransitFactory
import farebot.transit.troika.generated.resources.*

/**
 * Hybrid factory for Troika cards that may also contain Podorozhnik or Strelka.
 *
 * In Metrodroid, this is the ONLY registered Classic Troika factory — even standalone
 * Troika cards go through it (with null Podorozhnik/Strelka). This ensures hybrid
 * cards are always correctly detected and parsed as a composite.
 *
 * Faithful port of Metrodroid's TroikaHybridTransitData.FACTORY companion object.
 */
class TroikaHybridTransitFactory(
    private val stringResource: StringResource,
) : TransitFactory<ClassicCard, TroikaHybridTransitInfo> {
    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    private val troikaFactory = TroikaTransitFactory()
    private val podorozhnikFactory = PodorozhnikTransitFactory(stringResource)
    private val strelkaFactory = StrelkaTransitFactory()

    override fun check(card: ClassicCard): Boolean = troikaFactory.check(card)

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        // Check Podorozhnik first (takes priority), then Strelka, matching Metrodroid order
        val cardName =
            when {
                podorozhnikFactory.check(card) ->
                    getStringBlocking(Res.string.card_name_troika_podorozhnik_hybrid)
                strelkaFactory.check(card) ->
                    getStringBlocking(Res.string.card_name_troika_strelka_hybrid)
                else -> TroikaTransitFactory.CARD_NAME
            }

        // Serial number comes from Troika (shorter, printed larger on card)
        val troikaIdentity = troikaFactory.parseIdentity(card)
        return TransitIdentity.create(cardName, troikaIdentity.serialNumber)
    }

    override fun parseInfo(card: ClassicCard): TroikaHybridTransitInfo {
        val troika = troikaFactory.parseInfo(card)

        val podorozhnik =
            if (podorozhnikFactory.check(card)) {
                podorozhnikFactory.parseInfo(card)
            } else {
                null
            }

        val strelka =
            if (strelkaFactory.check(card)) {
                strelkaFactory.parseInfo(card)
            } else {
                null
            }

        return TroikaHybridTransitInfo(
            troika = troika,
            podorozhnik = podorozhnik,
            strelka = strelka,
        )
    }

    companion object {
        internal val CARD_INFO =
            CardInfo(
                nameRes = Res.string.card_name_troika,
                cardType = CardType.MifareClassic,
                region = TransitRegion.RUSSIA,
                locationRes = Res.string.troika_location,
                imageRes = Res.drawable.troika_card,
                latitude = 55.7558f,
                longitude = 37.6173f,
                brandColor = 0x1885A9,
                credits = listOf("Metrodroid Project", "Vladimir Serbinenko", "Michael Farrell"),
                sampleDumpFile = "Troika.json",
            )
    }
}
