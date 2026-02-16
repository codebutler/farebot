/*
 * UmarshTransitInfo.kt
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

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.ui.uiTree
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.zolotayakorona.RussiaTaxCodes
import farebot.transit.umarsh.generated.resources.Res
import farebot.transit.umarsh.generated.resources.umarsh_expiry_date
import farebot.transit.umarsh.generated.resources.umarsh_last_refill
import farebot.transit.umarsh.generated.resources.umarsh_machine_id
import farebot.transit.umarsh.generated.resources.umarsh_refill_counter
import farebot.transit.umarsh.generated.resources.umarsh_region
import kotlin.time.Instant

class UmarshTransitInfo(
    private val sectors: List<UmarshSector>,
    private val validation: UmarshTrip?,
) : TransitInfo() {
    override val serialNumber: String
        get() = formatSerial(sectors.first().serialNumber)

    override val cardName: FormattedString
        get() = sectors.first().cardName

    override val balances: List<TransitBalance>?
        get() = sectors.mapNotNull { it.balance }.ifEmpty { null }

    override val subscriptions: List<Subscription>?
        get() {
            val subs = sectors.filter { it.denomination != UmarshDenomination.RUB }
            if (subs.isEmpty()) return null
            return subs.map { sec ->
                UmarshSubscription(sec)
            }
        }

    override val info: List<ListItemInterface>
        get() =
            sectors.flatMap { sec ->
                listOf(
                    ListItem(Res.string.umarsh_refill_counter, sec.refillCounter.toString()),
                    ListItem(Res.string.umarsh_expiry_date, sec.cardExpiry?.toString() ?: ""),
                    ListItem(Res.string.umarsh_region, RussiaTaxCodes.codeToName(sec.region)),
                ) +
                    if (sec.denomination == UmarshDenomination.RUB) {
                        listOf(
                            ListItem(Res.string.umarsh_last_refill, sec.lastRefill?.toString() ?: ""),
                        )
                    } else {
                        emptyList()
                    }
            }

    override suspend fun getAdvancedUi(): FareBotUiTree? {
        val rubSectors = sectors.filter { it.denomination == UmarshDenomination.RUB }
        if (rubSectors.isEmpty()) return null
        return uiTree {
            for (sec in rubSectors) {
                item {
                    title = Res.string.umarsh_machine_id
                    value = sec.machineId.toString()
                }
            }
        }
    }

    override val trips: List<Trip>?
        get() = validation?.let { listOf(it) }

    companion object {
        private fun formatSerial(sn: Int): String = NumberUtils.formatNumber(sn.toLong(), " ", 3, 3, 3)
    }
}

private class UmarshSubscription(
    private val sector: UmarshSector,
) : Subscription() {
    override val subscriptionName: FormattedString?
        get() = sector.subscriptionName

    override val remainingTripCount: Int?
        get() = sector.remainingTripCount

    override val totalTripCount: Int?
        get() = sector.totalTripCount

    override val validTo: Instant?
        get() = sector.subscriptionValidTo
}
