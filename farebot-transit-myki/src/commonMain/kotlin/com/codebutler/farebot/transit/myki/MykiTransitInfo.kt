/*
 * MykiTransitInfo.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.myki

import com.codebutler.farebot.transit.TransitInfo
import farebot.farebot_transit_myki.generated.resources.Res
import farebot.farebot_transit_myki.generated.resources.myki_card_name
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Transit data type for Myki (Melbourne, AU).
 *
 * This is a very limited implementation of reading Myki, because most of the data is stored in
 * locked files.
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Myki
 */
class MykiTransitInfo(
    private val serialNumberValue: String
) : TransitInfo() {

    companion object {
        const val NAME = "Myki"

        fun create(serialNumber: String): MykiTransitInfo {
            return MykiTransitInfo(serialNumber)
        }
    }

    override val serialNumber: String? = serialNumberValue

    override val cardName: String = runBlocking { getString(Res.string.myki_card_name) }
}
