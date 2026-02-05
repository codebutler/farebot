/*
 * LaxTapData.kt
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.lax_tap

/**
 * Static data structures for LAX TAP.
 */
object LaxTapData {
    internal const val AGENCY_METRO = 1
    internal const val AGENCY_SANTA_MONICA = 11
    internal const val LAX_TAP_STR = "lax_tap"

    // Metro has Subway, Light Rail and Bus services, but all on the same Agency ID on the card.
    // Subway services are < LR_START, and Light Rail services are between LR_START and BUS_START.
    internal const val METRO_LR_START = 0x0100
    internal const val METRO_BUS_START = 0x8000

    /**
     * Map representing the different bus routes for Metro. We don't use the GTFS data for this one,
     * as the card differentiates codes based on direction (GTFS does not), GTFS data merges several
     * routes together as one, and there aren't that many bus routes that Metro run.
     */
    internal val METRO_BUS_ROUTES = mapOf(
        32788 to "733 East",
        32952 to "720 West",
        33055 to "733 West"
    )

    /**
     * Expected bytes in sector 0, block 1 (bytes 1..14) for LAX TAP identification.
     */
    internal val BLOCK1 = byteArrayOf(
        0x16, 0x18, 0x1A, 0x1B,
        0x1C, 0x1D, 0x1E, 0x1F,
        0x01, 0x01, 0x01, 0x01,
        0x01, 0x01
    )

    /**
     * Expected bytes in sector 0, block 2 (bytes 0..3) for LAX TAP identification.
     */
    internal val BLOCK2 = ByteArray(4)
}
