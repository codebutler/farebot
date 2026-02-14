/*
 * NdefVicinityTransitFactory.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.ndef

import com.codebutler.farebot.card.vicinity.VicinityCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

class NdefVicinityTransitFactory : TransitFactory<VicinityCard, NdefData> {
    override val allCards: List<CardInfo> = emptyList()

    override fun parseIdentity(card: VicinityCard): TransitIdentity = TransitIdentity.create(NdefData.NAME, null)

    override fun parseInfo(card: VicinityCard): NdefData = NdefData.parseVicinity(card) ?: NdefData(emptyList())

    override fun check(card: VicinityCard): Boolean = NdefData.checkVicinity(card)
}
