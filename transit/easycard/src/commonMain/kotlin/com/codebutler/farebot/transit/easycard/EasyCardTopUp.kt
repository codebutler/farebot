/*
 * EasyCardTopUp.kt
 *
 * Copyright 2017 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * Based on code from:
 * - http://www.fuzzysecurity.com/tutorials/rfid/4.html
 * - Farebot <https://codebutler.github.io/farebot/>
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

package com.codebutler.farebot.transit.easycard

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import kotlin.time.Instant

/**
 * Represents a top-up (refill) on an EasyCard.
 */
data class EasyCardTopUp(
    private val timestampRaw: Long,
    private val amount: Int,
    private val location: Int,
    private val machineIdRaw: Long,
) : Trip() {
    constructor(data: ByteArray) : this(
        timestampRaw = data.byteArrayToLongReversed(1, 4),
        amount = data.byteArrayToIntReversed(6, 2),
        location = data[11].toInt() and 0xFF,
        machineIdRaw = data.byteArrayToLongReversed(12, 4),
    )

    // Negative fare indicates money added to card
    override val fare: TransitCurrency get() = TransitCurrency.TWD(-amount)

    override val startTimestamp: Instant? get() = EasyCardTransitFactory.parseTimestamp(timestampRaw)

    override val startStation: Station?
        get() = EasyCardTransitFactory.lookupStation(location)

    override val mode: Mode get() = Mode.TICKET_MACHINE

    override val routeName: FormattedString? get() = null

    override val humanReadableRouteID: String? get() = null

    override val machineID: String get() = "0x${machineIdRaw.toString(16)}"

    companion object {
        /**
         * Parse the top-up record from sector 2, block 2.
         */
        fun parse(card: ClassicCard): EasyCardTopUp? {
            val data =
                (card.getSector(2) as? DataClassicSector)?.getBlock(2)?.data
                    ?: return null
            // Check if block is empty (all zeros)
            if (data.all { it == 0.toByte() }) {
                return null
            }
            return EasyCardTopUp(data)
        }
    }
}
