/*
 * TroikaTransitInfo.kt
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

import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.troika.generated.resources.Res
import farebot.transit.troika.generated.resources.card_name_troika
import farebot.transit.troika.generated.resources.troika_unformatted
import com.codebutler.farebot.base.util.FormattedString

/**
 * Troika, Moscow, Russia.
 * Multi-layout Classic card with data from sectors 8, 7, 4, 1.
 *
 * Each sector may contain a different TroikaBlock layout (purse, subscription, etc.).
 * The first valid sector determines the serial number and primary balance.
 */
class TroikaTransitInfo internal constructor(
    private val blocks: List<Pair<Int, TroikaBlock>>,
) : TransitInfo() {
    override val cardName: FormattedString
        get() = FormattedString(Res.string.card_name_troika)

    override val serialNumber: String?
        get() = blocks.firstOrNull()?.second?.serialNumber

    override val balance: TransitBalance?
        get() =
            blocks.firstOrNull()?.second?.balance
                ?: TransitBalance(balance = TransitCurrency.RUB(0))

    override val trips: List<Trip>
        get() = blocks.flatMap { (_, block) -> block.trips }

    override val subscriptions: List<Subscription>
        get() = blocks.mapNotNull { (_, block) -> block.subscription }

    override val info: List<ListItemInterface>?
        get() = blocks.flatMap { (_, block) -> block.info.orEmpty() }.ifEmpty { null }

    override val warning: FormattedString?
        get() =
            if (blocks.firstOrNull()?.second?.balance == null && subscriptions.isEmpty()) {
                FormattedString(Res.string.troika_unformatted)
            } else {
                null
            }
}
