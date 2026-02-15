/*
 * OVChipTransitInfo.kt
 *
 * Copyright 2012-2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.ovc

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.en1545.En1545FixedInteger
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545TransitData
import farebot.transit.ovc.generated.resources.*
import kotlinx.datetime.TimeZone
import com.codebutler.farebot.base.util.FormattedString

class OVChipTransitInfo(
    private val parsed: En1545Parsed,
    private val index: OVChipIndex,
    private val expdate: Int,
    private val type: Int,
    private val creditSlotId: Int,
    private val creditId: Int,
    private val credit: Int,
    private val banbits: Int,
    override val trips: List<Trip>,
    override val subscriptions: List<Subscription>,
) : TransitInfo() {
    override val cardName: FormattedString = NAME

    override val balance: TransitBalance
        get() =
            TransitBalance(
                balance = TransitCurrency.EUR(credit),
                name =
                    if (type == 2) {
                        "Personal"
                    } else {
                        "Anonymous"
                    },
                validTo = convertDate(expdate),
            )

    override val serialNumber: String? get() = null

    private val lookup: En1545Lookup get() = OvcLookup

    override val info: List<ListItemInterface>
        get() {
            val li = mutableListOf<ListItemInterface>()
            val tz = lookup.timeZone

            // EN1545 standard info fields
            if (parsed.contains(En1545FixedInteger.dateBCDName(En1545TransitData.HOLDER_BIRTH_DATE))) {
                parsed.getTimeStamp(En1545TransitData.HOLDER_BIRTH_DATE, tz)?.let {
                    li.add(
                        ListItem(
                            Res.string.ovc_birthdate,
                            com.codebutler.farebot.base.util.formatDate(
                                it,
                                com.codebutler.farebot.base.util.DateFormatStyle.LONG,
                            ),
                        ),
                    )
                }
            }

            if (parsed.getIntOrZero(En1545TransitData.ENV_APPLICATION_ISSUER_ID) != 0) {
                val issuerName =
                    lookup.getAgencyName(
                        parsed.getIntOrZero(En1545TransitData.ENV_APPLICATION_ISSUER_ID),
                        false,
                    )
                if (issuerName != null) {
                    li.add(ListItem(Res.string.ovc_issuer, issuerName))
                }
            }

            // OVC-specific info
            li.add(
                ListItem(
                    Res.string.ovc_banned,
                    if (banbits and 0xC0 == 0xC0) {
                        FormattedString(Res.string.ovc_yes)
                    } else {
                        FormattedString(Res.string.ovc_no)
                    },
                ),
            )

            li.add(HeaderListItem(Res.string.ovc_autocharge_information))
            li.add(
                ListItem(
                    Res.string.ovc_autocharge,
                    if (parsed.getIntOrZero(OVChipTransitFactory.AUTOCHARGE_ACTIVE) == 0x05) {
                        FormattedString(Res.string.ovc_yes)
                    } else {
                        FormattedString(Res.string.ovc_no)
                    },
                ),
            )
            li.add(
                ListItem(
                    Res.string.ovc_autocharge_limit,
                    TransitCurrency
                        .EUR(parsed.getIntOrZero(OVChipTransitFactory.AUTOCHARGE_LIMIT))
                        .formatCurrencyString(true),
                ),
            )
            li.add(
                ListItem(
                    Res.string.ovc_autocharge_charge,
                    TransitCurrency
                        .EUR(parsed.getIntOrZero(OVChipTransitFactory.AUTOCHARGE_CHARGE))
                        .formatCurrencyString(true),
                ),
            )

            return li
        }

    override suspend fun getAdvancedUi(): FareBotUiTree {
        val b = FareBotUiTree.builder()
        b.item().title("Credit Slot ID").value(creditSlotId.toString())
        b.item().title("Last Credit ID").value(creditId.toString())
        val slotsItem = b.item().title("Recent Slots")
        index.addAdvancedItems(slotsItem)
        return b.build()
    }

    companion object {
        private val NAME = FormattedString("OV-chipkaart")

        fun convertDate(date: Int) = En1545FixedInteger.parseDate(date, TimeZone.of("Europe/Amsterdam"))
    }
}
