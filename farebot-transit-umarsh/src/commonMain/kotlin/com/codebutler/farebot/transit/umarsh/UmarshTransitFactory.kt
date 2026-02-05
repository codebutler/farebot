/*
 * UmarshTransitFactory.kt
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

package com.codebutler.farebot.transit.umarsh

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

class UmarshTransitFactory : TransitFactory<ClassicCard, UmarshTransitInfo> {

    override fun check(card: ClassicCard): Boolean {
        val sector8 = card.getSector(8)
        if (sector8 !is DataClassicSector) return false
        return UmarshSector.check(sector8)
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val sec = UmarshSector.parse(card.getSector(8) as DataClassicSector, 8)
        return TransitIdentity.create(
            sec.cardName,
            NumberUtils.formatNumber(sec.serialNumber.toLong(), " ", 3, 3, 3)
        )
    }

    override fun parseInfo(card: ClassicCard): UmarshTransitInfo {
        val sec8 = UmarshSector.parse(card.getSector(8) as DataClassicSector, 8)
        val secs = if (!sec8.hasExtraSector)
            listOf(sec8)
        else
            listOf(sec8, UmarshSector.parse(card.getSector(7) as DataClassicSector, 7))

        val validationData = (card.getSector(0) as? DataClassicSector)?.getBlock(1)?.data
        val validation = if (validationData != null) UmarshTrip.parse(validationData, sec8.region) else null

        return UmarshTransitInfo(secs, validation)
    }
}
