/*
 * PiletTransitInfo.kt
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

package com.codebutler.farebot.transit.pilet

import com.codebutler.farebot.transit.TransitInfo

/**
 * Transit data type for Pilet-based cards (Tartu Bus, Kyiv Digital).
 *
 * These are serial-only cards backed by pilet.ee NDEF records on MIFARE Classic.
 * Card number is extracted from the NDEF TLV payload on the card.
 */
class PiletTransitInfo(
    private val serial: String?,
    override val cardName: String
) : TransitInfo() {

    override val serialNumber: String?
        get() = serial
}
