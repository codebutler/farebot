/*
 * EZLinkTrip.kt
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Victor Heng
 * Copyright 2012 Toby Bonang
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.ezlink

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.card.cepas.CEPASTransaction
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import farebot.transit.ezlink.generated.resources.*
import kotlin.time.Instant

private fun String.toStationOrNull(): Station? = trim().ifEmpty { null }?.let { Station.nameOnly(it) }

internal data class EZUserData(
    val startStation: Station?,
    val endStation: Station?,
    val routeName: FormattedString,
) {
    companion object {
        fun parse(
            userData: String,
            type: CEPASTransaction.TransactionType,
        ): EZUserData {
            if ((type == CEPASTransaction.TransactionType.BUS || type == CEPASTransaction.TransactionType.BUS_REFUND) &&
                (userData.startsWith("SVC") || userData.startsWith("BUS"))
            ) {
                val routeName =
                    if (type == CEPASTransaction.TransactionType.BUS_REFUND) {
                        FormattedString(Res.string.ez_bus_refund)
                    } else {
                        FormattedString(Res.string.ez_bus_number, userData.substring(3, 7).replace(" ", ""))
                    }
                return EZUserData(
                    startStation = null,
                    endStation = null,
                    routeName = routeName,
                )
            }
            if (type == CEPASTransaction.TransactionType.CREATION) {
                return EZUserData(userData.toStationOrNull(), null, FormattedString(Res.string.ez_first_use))
            }
            if (type == CEPASTransaction.TransactionType.RETAIL) {
                return EZUserData(
                    userData.toStationOrNull(),
                    null,
                    FormattedString(Res.string.ez_retail_purchase),
                )
            }

            val routeName =
                when (type) {
                    CEPASTransaction.TransactionType.BUS ->
                        FormattedString(
                            Res.string.ez_unknown_format,
                            userData,
                        )
                    CEPASTransaction.TransactionType.BUS_REFUND -> FormattedString(Res.string.ez_bus_refund)
                    CEPASTransaction.TransactionType.MRT -> FormattedString(Res.string.ez_mrt)
                    CEPASTransaction.TransactionType.TOP_UP -> FormattedString(Res.string.ez_topup)
                    CEPASTransaction.TransactionType.SERVICE -> FormattedString(Res.string.ez_service_charge)
                    else -> FormattedString(Res.string.ez_unknown_format, type.toString())
                }

            if (userData.length > 6 && (userData[3] == '-' || userData[3] == ' ')) {
                val startStationAbbr = userData.substring(0, 3)
                val endStationAbbr = userData.substring(4, 7)
                return EZUserData(
                    EZLinkData.getStation(startStationAbbr),
                    EZLinkData.getStation(endStationAbbr),
                    routeName,
                )
            }
            return EZUserData(userData.toStationOrNull(), null, routeName)
        }
    }
}

class EZLinkTrip(
    private val transaction: CEPASTransaction,
    private val cardName: FormattedString,
) : Trip() {
    override val startTimestamp: Instant
        get() = Instant.fromEpochSeconds(transaction.timestamp.toLong())

    override val routeName: FormattedString
        get() = EZUserData.parse(transaction.userData, transaction.type).routeName

    override val humanReadableRouteID: String?
        get() = transaction.userData

    override val fare: TransitCurrency?
        get() =
            if (transaction.type == CEPASTransaction.TransactionType.CREATION) {
                null
            } else {
                TransitCurrency.SGD(-transaction.amount)
            }

    override val startStation: Station?
        get() = EZUserData.parse(transaction.userData, transaction.type).startStation

    override val endStation: Station?
        get() = EZUserData.parse(transaction.userData, transaction.type).endStation

    override val mode: Mode
        get() = getMode(transaction.type)

    override val agencyName: FormattedString
        get() = getAgencyName(transaction.type, cardName, isShort = false)

    override val shortAgencyName: FormattedString
        get() = getAgencyName(transaction.type, cardName, isShort = true)

    companion object {
        fun getMode(type: CEPASTransaction.TransactionType): Mode =
            when (type) {
                CEPASTransaction.TransactionType.BUS,
                CEPASTransaction.TransactionType.BUS_REFUND,
                -> Mode.BUS
                CEPASTransaction.TransactionType.MRT -> Mode.METRO
                CEPASTransaction.TransactionType.TOP_UP -> Mode.TICKET_MACHINE
                CEPASTransaction.TransactionType.RETAIL,
                CEPASTransaction.TransactionType.SERVICE,
                -> Mode.POS
                else -> Mode.OTHER
            }

        fun getAgencyName(
            type: CEPASTransaction.TransactionType,
            cardName: FormattedString,
            isShort: Boolean,
        ): FormattedString =
            when (type) {
                CEPASTransaction.TransactionType.BUS,
                CEPASTransaction.TransactionType.BUS_REFUND,
                -> FormattedString(Res.string.ezlink_agency_bus)
                CEPASTransaction.TransactionType.CREATION,
                CEPASTransaction.TransactionType.TOP_UP,
                CEPASTransaction.TransactionType.SERVICE,
                ->
                    if (isShort && cardName == FormattedString(Res.string.ezlink_issuer_ezlink)) {
                        FormattedString(Res.string.ezlink_agency_ez)
                    } else {
                        cardName
                    }
                CEPASTransaction.TransactionType.RETAIL -> FormattedString(Res.string.ezlink_agency_pos)
                else -> FormattedString(Res.string.ezlink_agency_smrt)
            }
    }
}
