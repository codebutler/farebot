/*
 * EasyCardTransitInfo.kt
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

import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_easycard.generated.resources.Res
import farebot.farebot_transit_easycard.generated.resources.easycard_card_name
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

data class EasyCardTransitInfo(
    private val balanceValue: Int,
    private val tripList: List<Trip>
) : TransitInfo() {

    override val cardName: String = runBlocking { getString(Res.string.easycard_card_name) }

    // EasyCard doesn't expose a serial number
    override val serialNumber: String? = null

    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.TWD(balanceValue))

    override val trips: List<Trip> = tripList

    override val subscriptions: List<Subscription> = listOf()

    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree? = null
}
