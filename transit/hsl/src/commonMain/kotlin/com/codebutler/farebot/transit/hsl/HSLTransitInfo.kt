/*
 * HSLTransitInfo.kt
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.hsl

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.base.util.FormattedString
import farebot.transit.hsl.generated.resources.*

class HSLTransitInfo(
    override val serialNumber: String?,
    private val mBalance: Int,
    override val trips: List<Trip>,
    override val subscriptions: List<Subscription>?,
    val applicationVersion: Int?,
    val applicationKeyVersion: Int?,
    val platformType: Int?,
    val securityLevel: Int?,
    val cardNameOverride: String,
) : TransitInfo() {
    override val cardName: FormattedString
        get() = FormattedString(cardNameOverride)

    override val balance: TransitBalance?
        get() = TransitBalance(balance = TransitCurrency.EUR(mBalance))

    override suspend fun getAdvancedUi(): FareBotUiTree? {
        val b = FareBotUiTree.builder()
        applicationVersion?.let { b.item().title(Res.string.hsl_application_version).value(it) }
        applicationKeyVersion?.let { b.item().title(Res.string.hsl_application_key_version).value(it) }
        platformType?.let { b.item().title(Res.string.hsl_platform_type).value(it) }
        securityLevel?.let { b.item().title(Res.string.hsl_security_level).value(it) }
        val tree = b.build()
        return if (tree.items.isEmpty()) null else tree
    }
}
