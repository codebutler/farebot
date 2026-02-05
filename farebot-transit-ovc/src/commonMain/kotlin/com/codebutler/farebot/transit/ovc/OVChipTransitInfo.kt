/*
 * OVChipTransitInfo.kt
 *
 * Copyright (C) 2012-2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.DateFormatStyle
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.formatDate
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_ovc.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class OVChipTransitInfo(
    override val trips: List<Trip>,
    override val subscriptions: List<Subscription>,
    private val index: OVChipIndex,
    private val preamble: OVChipPreamble,
    private val ovcInfo: OVChipInfo,
    private val credit: OVChipCredit,
    private val stringResource: StringResource,
) : TransitInfo() {

    override val cardName: String = runBlocking { getString(Res.string.ovc_card_name) }

    override val balance: TransitBalance
        get() = TransitBalance(
            balance = TransitCurrency.EUR(credit.credit),
            name = if (preamble.type == 2) runBlocking { getString(Res.string.ovc_personal) } else runBlocking { getString(Res.string.ovc_anonymous) },
            validTo = OVChipUtil.convertDate(preamble.expdate)
        )

    override val serialNumber: String?
        get() = preamble.id

    override val info: List<ListItemInterface>
        get() {
            val items = mutableListOf<ListItemInterface>()

            items.add(HeaderListItem(Res.string.ovc_hardware_information))
            items.add(ListItem(Res.string.ovc_manufacturer_id, preamble.manufacturer))
            items.add(ListItem(Res.string.ovc_publisher_id, preamble.publisher))

            items.add(HeaderListItem(Res.string.ovc_general_information))
            items.add(ListItem(Res.string.ovc_serial_number, preamble.id))
            items.add(ListItem(
                Res.string.ovc_expiration_date,
                formatDate(OVChipUtil.convertDate(preamble.expdate), DateFormatStyle.LONG),
            ))
            items.add(ListItem(Res.string.ovc_card_type, if (preamble.type == 2) runBlocking { getString(Res.string.ovc_personal) } else runBlocking { getString(Res.string.ovc_anonymous) }))
            items.add(ListItem(Res.string.ovc_issuer, getShortAgencyName(stringResource, ovcInfo.company)))
            items.add(ListItem(Res.string.ovc_banned, if ((credit.banbits and 0xC0) == 0xC0) runBlocking { getString(Res.string.ovc_yes) } else runBlocking { getString(Res.string.ovc_no) }))

            if (preamble.type == 2) {
                items.add(HeaderListItem(Res.string.ovc_personal_information))
                items.add(ListItem(Res.string.ovc_birthdate, formatDate(ovcInfo.birthdate, DateFormatStyle.LONG)))
            }

            items.add(HeaderListItem(Res.string.ovc_credit_information))
            items.add(ListItem(Res.string.ovc_credit_slot_id, credit.id.toString()))
            items.add(ListItem(Res.string.ovc_last_credit_id, credit.creditId.toString()))
            items.add(ListItem(Res.string.ovc_credit, OVChipUtil.convertAmount(credit.credit)))
            items.add(ListItem(Res.string.ovc_autocharge, if (ovcInfo.active == 0x05) runBlocking { getString(Res.string.ovc_yes) } else runBlocking { getString(Res.string.ovc_no) }))
            items.add(ListItem(Res.string.ovc_autocharge_limit, OVChipUtil.convertAmount(ovcInfo.limit)))
            items.add(ListItem(Res.string.ovc_autocharge_charge, OVChipUtil.convertAmount(ovcInfo.charge)))

            items.add(HeaderListItem(Res.string.ovc_recent_slots))
            items.add(ListItem(Res.string.ovc_transaction_slot, "0x${index.recentTransactionSlot.toChar().code.toString(16)}"))
            items.add(ListItem(Res.string.ovc_info_slot, "0x${index.recentInfoSlot.toChar().code.toString(16)}"))
            items.add(ListItem(Res.string.ovc_subscription_slot, "0x${index.recentSubscriptionSlot.toChar().code.toString(16)}"))
            items.add(ListItem(Res.string.ovc_travelhistory_slot, "0x${index.recentTravelhistorySlot.toChar().code.toString(16)}"))
            items.add(ListItem(Res.string.ovc_credit_slot, "0x${index.recentCreditSlot.toChar().code.toString(16)}"))

            return items
        }

    companion object {
        const val PROCESS_PURCHASE = 0x00
        const val PROCESS_CHECKIN = 0x01
        const val PROCESS_CHECKOUT = 0x02
        const val PROCESS_TRANSFER = 0x06
        const val PROCESS_BANNED = 0x07
        const val PROCESS_CREDIT = -0x02
        const val PROCESS_NODATA = -0x03

        const val AGENCY_TLS = 0x00
        const val AGENCY_CONNEXXION = 0x01
        const val AGENCY_GVB = 0x02
        const val AGENCY_HTM = 0x03
        const val AGENCY_NS = 0x04
        const val AGENCY_RET = 0x05
        const val AGENCY_VEOLIA = 0x07
        const val AGENCY_ARRIVA = 0x08
        const val AGENCY_SYNTUS = 0x09
        const val AGENCY_QBUZZ = 0x0A
        const val AGENCY_DUO = 0x0C
        const val AGENCY_STORE = 0x19
        const val AGENCY_DUO_ALT = 0x2C

        private val sAgencies: Map<Int, String> = mapOf(
            AGENCY_TLS to "Trans Link Systems",
            AGENCY_CONNEXXION to "Connexxion",
            AGENCY_GVB to "Gemeentelijk Vervoersbedrijf",
            AGENCY_HTM to "Haagsche Tramweg-Maatschappij",
            AGENCY_NS to "Nederlandse Spoorwegen",
            AGENCY_RET to "Rotterdamse Elektrische Tram",
            AGENCY_VEOLIA to "Veolia",
            AGENCY_ARRIVA to "Arriva",
            AGENCY_SYNTUS to "Syntus",
            AGENCY_QBUZZ to "Qbuzz",
            AGENCY_DUO to "Dienst Uitvoering Onderwijs",
            AGENCY_STORE to "Reseller",
            AGENCY_DUO_ALT to "Dienst Uitvoering Onderwijs"
        )

        private val sShortAgencies: Map<Int, String> = mapOf(
            AGENCY_TLS to "TLS",
            AGENCY_CONNEXXION to "Connexxion",
            AGENCY_GVB to "GVB",
            AGENCY_HTM to "HTM",
            AGENCY_NS to "NS",
            AGENCY_RET to "RET",
            AGENCY_VEOLIA to "Veolia",
            AGENCY_ARRIVA to "Arriva",
            AGENCY_SYNTUS to "Syntus",
            AGENCY_QBUZZ to "Qbuzz",
            AGENCY_DUO to "DUO",
            AGENCY_STORE to "Reseller",
            AGENCY_DUO_ALT to "DUO"
        )

        fun getAgencyName(stringResource: StringResource, agency: Int): String {
            return sAgencies[agency]
                ?: stringResource.getString(Res.string.ovc_unknown_format, "0x" + agency.toLong().toString(16))
        }

        fun getAgencyName(agency: Int): String {
            return sAgencies[agency] ?: "Unknown (0x${agency.toLong().toString(16)})"
        }

        fun getShortAgencyName(stringResource: StringResource, agency: Int): String {
            return sShortAgencies[agency]
                ?: stringResource.getString(Res.string.ovc_unknown_format, "0x" + agency.toLong().toString(16))
        }

        fun getShortAgencyName(agency: Int): String {
            return sShortAgencies[agency] ?: "Unknown (0x${agency.toLong().toString(16)})"
        }
    }
}
