/*
 * HSLTransitInfo.kt
 *
 * Copyright (C) 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
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

import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.DateFormatStyle
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.formatDate
import com.codebutler.farebot.base.util.formatDateTime
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_hsl.generated.resources.*
import kotlin.time.Instant

class HSLTransitInfo(
    override val serialNumber: String,
    override val trips: List<Trip>,
    private val balanceValue: Int,
    private val applicationVersion: Int,
    private val applicationKeyVersion: Int,
    private val platformType: Int,
    private val securityLevel: Int,
    private val hasKausi: Boolean,
    private val kausiStart: Long,
    private val kausiEnd: Long,
    private val kausiPrevStart: Long,
    private val kausiPrevEnd: Long,
    private val kausiPurchasePrice: Long,
    private val kausiLastUse: Long,
    private val kausiPurchase: Long,
    private val kausiNoData: Boolean,
    private val arvoExit: Long,
    private val arvoPurchase: Long,
    private val arvoExpire: Long,
    private val arvoPax: Long,
    private val arvoPurchasePrice: Long,
    private val arvoXfer: Long,
    private val arvoDiscoGroup: Long,
    private val arvoMystery1: Long,
    private val arvoDuration: Long,
    private val arvoRegional: Long,
    private val arvoJOREExt: Long,
    private val arvoVehicleNumber: Long,
    private val arvoUnknown: Long,
    private val arvoLineJORE: Long,
    private val kausiVehicleNumber: Long,
    private val kausiUnknown: Long,
    private val kausiLineJORE: Long,
    private val kausiJOREExt: Long,
    private val arvoDirection: Long,
    private val kausiDirection: Long,
    private val stringResource: StringResource,
) : TransitInfo() {

    companion object {
        private val REGION_NAMES = arrayOf(
            "N/A", "Helsinki", "Espoo", "Vantaa", "Koko alue", "Seutu", "", "", "", "",
            "", "", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "", "", ""
        )

        fun formatSerial(serial: String): String {
            if (serial.length < 18) return serial
            return "${serial.substring(0, 6)} ${serial.substring(6, 10)} ${serial.substring(10, 14)} ${serial.substring(14, 18)}"
        }
    }

    override val cardName: String = "HSL"

    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.EUR(balanceValue))

    override val info: List<ListItemInterface>
        get() {
            val items = mutableListOf<ListItemInterface>()

            items.add(HeaderListItem(Res.string.hsl_card_information))
            items.add(ListItem(Res.string.hsl_application_version, applicationVersion.toString()))
            items.add(ListItem(Res.string.hsl_application_key_version, applicationKeyVersion.toString()))
            items.add(ListItem(Res.string.hsl_platform_type, platformType.toString()))
            items.add(ListItem(Res.string.hsl_security_level, securityLevel.toString()))

            if (!kausiNoData) {
                items.add(HeaderListItem(stringResource.getString(Res.string.hsl_season_ticket)))
                items.add(ListItem(
                    stringResource.getString(Res.string.hsl_value_ticket_vehicle_number),
                    kausiVehicleNumber.toString(),
                ))
                items.add(ListItem(
                    stringResource.getString(Res.string.hsl_value_ticket_line_number),
                    kausiLineJORE.toString().substring(1),
                ))
                items.add(ListItem(Res.string.hsl_jore_extension, kausiJOREExt.toString()))
                items.add(ListItem(Res.string.hsl_direction, kausiDirection.toString()))
                items.add(ListItem(
                    stringResource.getString(Res.string.hsl_season_ticket_starts),
                    formatDate(Instant.fromEpochSeconds(kausiStart), DateFormatStyle.SHORT),
                ))
                items.add(ListItem(
                    stringResource.getString(Res.string.hsl_season_ticket_ends),
                    formatDate(Instant.fromEpochSeconds(kausiEnd), DateFormatStyle.SHORT),
                ))
                items.add(ListItem(
                    stringResource.getString(Res.string.hsl_season_ticket_bought_on),
                    formatDateTime(Instant.fromEpochSeconds(kausiPurchase), DateFormatStyle.SHORT, DateFormatStyle.SHORT),
                ))
                items.add(ListItem(
                    stringResource.getString(Res.string.hsl_season_ticket_price_was),
                    TransitCurrency.EUR(kausiPurchasePrice.toInt()).formatCurrencyString(),
                ))
                items.add(ListItem(
                    stringResource.getString(Res.string.hsl_you_last_used_this_ticket),
                    formatDateTime(Instant.fromEpochSeconds(kausiLastUse), DateFormatStyle.SHORT, DateFormatStyle.SHORT),
                ))
                items.add(ListItem(
                    stringResource.getString(Res.string.hsl_previous_season_ticket),
                    "${formatDate(Instant.fromEpochSeconds(kausiPrevStart), DateFormatStyle.SHORT)} - ${formatDate(Instant.fromEpochSeconds(kausiPrevEnd), DateFormatStyle.SHORT)}",
                ))
            }

            items.add(HeaderListItem(stringResource.getString(Res.string.hsl_value_ticket)))
            items.add(ListItem(
                stringResource.getString(Res.string.hsl_value_ticket_bought_on),
                formatDateTime(Instant.fromEpochSeconds(arvoPurchase), DateFormatStyle.SHORT, DateFormatStyle.SHORT),
            ))
            items.add(ListItem(
                stringResource.getString(Res.string.hsl_value_ticket_expires_on),
                formatDateTime(Instant.fromEpochSeconds(arvoExpire), DateFormatStyle.SHORT, DateFormatStyle.SHORT),
            ))
            items.add(ListItem(
                stringResource.getString(Res.string.hsl_value_ticket_last_transfer),
                formatDateTime(Instant.fromEpochSeconds(arvoXfer), DateFormatStyle.SHORT, DateFormatStyle.SHORT),
            ))
            items.add(ListItem(
                stringResource.getString(Res.string.hsl_value_ticket_last_sign),
                formatDateTime(Instant.fromEpochSeconds(arvoExit), DateFormatStyle.SHORT, DateFormatStyle.SHORT),
            ))
            items.add(ListItem(
                stringResource.getString(Res.string.hsl_value_ticket_price),
                TransitCurrency.EUR(arvoPurchasePrice.toInt()).formatCurrencyString(),
            ))
            items.add(ListItem(
                stringResource.getString(Res.string.hsl_value_ticket_disco_group),
                arvoDiscoGroup.toString(),
            ))
            items.add(ListItem(
                stringResource.getString(Res.string.hsl_value_ticket_pax),
                arvoPax.toString(),
            ))
            items.add(ListItem(Res.string.hsl_mystery1, arvoMystery1.toString()))
            items.add(ListItem(
                stringResource.getString(Res.string.hsl_value_ticket_duration),
                "$arvoDuration min",
            ))
            items.add(ListItem(
                stringResource.getString(Res.string.hsl_value_ticket_vehicle_number),
                arvoVehicleNumber.toString(),
            ))
            items.add(ListItem(
                Res.string.hsl_region,
                REGION_NAMES[arvoRegional.toInt()],
            ))
            items.add(ListItem(
                stringResource.getString(Res.string.hsl_value_ticket_line_number),
                arvoLineJORE.toString().substring(1),
            ))
            items.add(ListItem(Res.string.hsl_jore_extension, arvoJOREExt.toString()))
            items.add(ListItem(Res.string.hsl_direction, arvoDirection.toString()))

            return items
        }
}
