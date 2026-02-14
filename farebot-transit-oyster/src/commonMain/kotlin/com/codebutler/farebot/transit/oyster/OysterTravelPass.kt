/*
 * OysterTravelPass.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.oyster

import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.base.util.isAllZero
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitCurrency
import farebot.farebot_transit_oyster.generated.resources.Res
import farebot.farebot_transit_oyster.generated.resources.oyster_travelpass
import kotlin.time.Instant

class OysterTravelPass(
    override val validFrom: Instant,
    override val validTo: Instant,
    override val cost: TransitCurrency,
) : Subscription() {
    // TODO: Figure this out properly.
    override val subscriptionName: String
        get() = getStringBlocking(Res.string.oyster_travelpass)

    companion object {
        internal fun parseAll(card: ClassicCard): List<OysterTravelPass> {
            val result = mutableListOf<OysterTravelPass>()
            for (block in 0..2) {
                try {
                    val sec7 =
                        (card.getSector(7) as? DataClassicSector)
                            ?.getBlock(block)
                            ?.data ?: continue

                    // Don't know what a blank card looks like, so try to skip if it doesn't look
                    // like there is any expiry date on a pass.
                    if (sec7.sliceOffLen(9, 4).isAllZero()) {
                        // invalid date?
                        continue
                    }

                    val sec8 =
                        (card.getSector(8) as? DataClassicSector)
                            ?.getBlock(block)
                            ?.data ?: continue

                    result.add(
                        OysterTravelPass(
                            validFrom = OysterUtils.parseTimestamp(sec8, 78),
                            validTo = OysterUtils.parseTimestamp(sec7, 33),
                            cost = TransitCurrency.GBP(sec8.byteArrayToIntReversed(0, 2)),
                        ),
                    )
                } catch (_: Exception) {
                }
            }
            return result
        }
    }
}
