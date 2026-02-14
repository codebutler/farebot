/*
 * TroikaHybridTransitInfo.kt
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

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.podorozhnik.PodorozhnikTransitInfo
import com.codebutler.farebot.transit.serialonly.StrelkaTransitInfo
import farebot.transit.troika.generated.resources.Res
import farebot.transit.troika.generated.resources.card_name_troika_podorozhnik_hybrid
import farebot.transit.troika.generated.resources.card_name_troika_strelka_hybrid
import farebot.transit.troika.generated.resources.card_number

/**
 * Hybrid cards containing both Troika and Podorozhnik or Strelka.
 *
 * Faithful port of Metrodroid's TroikaHybridTransitData.
 */
class TroikaHybridTransitInfo(
    private val troika: TroikaTransitInfo,
    private val podorozhnik: PodorozhnikTransitInfo?,
    private val strelka: StrelkaTransitInfo?,
) : TransitInfo() {
    override val serialNumber: String?
        get() = troika.serialNumber

    // This is Podorozhnik/Strelka serial number. Combined card
    // has both serial numbers and both are printed on it.
    // We show Troika number as main serial as it's shorter
    // and printed in larger letters.
    override val info: List<ListItemInterface>?
        get() {
            val items = mutableListOf<ListItemInterface>()

            val troikaItems = troika.info

            if (troikaItems != null && troikaItems.isNotEmpty()) {
                items.add(HeaderListItem(troika.cardName))
                items.addAll(troikaItems)
            }

            if (podorozhnik != null) {
                items.add(HeaderListItem(podorozhnik.cardName))
                items.add(ListItem(Res.string.card_number, podorozhnik.serialNumber))

                items += podorozhnik.info.orEmpty()
            }

            if (strelka != null) {
                items.add(HeaderListItem(strelka.cardName))
                items.add(ListItem(Res.string.card_number, strelka.serialNumber))
            }

            return items.ifEmpty { null }
        }

    override val cardName: String
        get() {
            if (podorozhnik != null) {
                return getStringBlocking(Res.string.card_name_troika_podorozhnik_hybrid)
            }
            if (strelka != null) {
                return getStringBlocking(Res.string.card_name_troika_strelka_hybrid)
            }
            return troika.cardName
        }

    override val trips: List<Trip>
        get() = podorozhnik?.trips.orEmpty() + troika.trips

    override val balances: List<TransitBalance>?
        get() = (troika.balances.orEmpty() + podorozhnik?.balances.orEmpty()).ifEmpty { null }

    override val subscriptions: List<Subscription>?
        get() = troika.subscriptions

    override val warning: String?
        get() = troika.warning

    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree? {
        val trees =
            listOfNotNull(
                troika.getAdvancedUi(stringResource),
                podorozhnik?.getAdvancedUi(stringResource),
                strelka?.getAdvancedUi(stringResource),
            )
        if (trees.isEmpty()) return null
        val b = FareBotUiTree.builder(stringResource)
        for (tree in trees) {
            for (item in tree.items) {
                b.item().title(item.title).value(item.value)
            }
        }
        return b.build()
    }
}
