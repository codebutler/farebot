/*
 * ZolotayaKoronaTransitInfo.kt
 *
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

package com.codebutler.farebot.transit.zolotayakorona

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_zolotayakorona.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Instant

class ZolotayaKoronaTransitInfo internal constructor(
    private val serial: String,
    private val balanceValue: Int?,
    private val cardSerial: String,
    private val trip: ZolotayaKoronaTrip?,
    private val refill: ZolotayaKoronaRefill?,
    private val cardType: Int,
    private val status: Int,
    private val discountCode: Int,
    private val sequenceCtr: Int,
    private val tail: ByteArray,
) : TransitInfo() {
    companion object {
        private val discountMap =
            mapOf<Int, StringResource>(
                0x46 to Res.string.zolotaya_korona_discount_111,
                0x47 to Res.string.zolotaya_korona_discount_100,
                0x48 to Res.string.zolotaya_korona_discount_200,
            )

        private val CARD_NAMES =
            mapOf<Int, StringResource>(
                0x230100 to Res.string.card_name_krasnodar_etk,
                0x560200 to Res.string.card_name_orenburg_ekg,
                0x562300 to Res.string.card_name_orenburg_school,
                0x562400 to Res.string.card_name_orenburg_student,
                0x631500 to Res.string.card_name_samara_school,
                0x632600 to Res.string.card_name_samara_etk,
                0x632700 to Res.string.card_name_samara_student,
                0x633500 to Res.string.card_name_samara_garden_dacha,
                0x760500 to Res.string.card_name_yaroslavl_etk,
            )

        internal fun nameCard(type: Int): String {
            val cardNameRes = CARD_NAMES[type]
            return if (cardNameRes != null) {
                getStringBlocking(cardNameRes)
            } else {
                val baseName = getStringBlocking(Res.string.zolotaya_korona_card_name)
                "$baseName ${type.toString(16)}"
            }
        }

        fun parseTime(
            time: Int,
            cardType: Int,
        ): Instant? {
            if (time == 0) return null
            val tz = RussiaTaxCodes.BCDToTimeZone(cardType shr 16)
            // This is pseudo unix time with local day always coerced to 86400 seconds
            val daysSinceEpoch = time / 86400
            val secondsInDay = time % 86400
            val hours = secondsInDay / 3600
            val minutes = (secondsInDay % 3600) / 60
            val seconds = secondsInDay % 60
            // Compute the date from days since epoch (1970-01-01)
            val epochDate = LocalDate(1970, 1, 1)
            val date = LocalDate.fromEpochDays(daysSinceEpoch)
            val ldt = LocalDateTime(date.year, date.month, date.day, hours, minutes, seconds)
            return ldt.toInstant(tz)
        }

        fun formatSerial(serial: String): String = NumberUtils.groupString(serial, " ", 4, 5, 5)
    }

    private val estimatedBalance: Int
        get() {
            // a trip followed by refill. Assume only one refill.
            if (refill != null && trip != null && refill.time > trip.time) {
                return trip.estimatedBalance + refill.amount
            }
            // Last transaction was a trip
            if (trip != null) {
                return trip.estimatedBalance
            }
            // No trips. Look for refill
            if (refill != null) {
                return refill.amount
            }
            // Card was never used or refilled
            return 0
        }

    override val balance: TransitBalance
        get() {
            val bal =
                if (balanceValue ==
                    null
                ) {
                    TransitCurrency.RUB(estimatedBalance)
                } else {
                    TransitCurrency.RUB(balanceValue)
                }
            return TransitBalance(balance = bal)
        }

    override val serialNumber: String get() = formatSerial(serial)

    override val cardName: String get() = nameCard(cardType)

    override val info: List<ListItemInterface>
        get() {
            val regionNum = cardType shr 16
            val regionName = RussiaTaxCodes.BCDToName(regionNum)
            val discountName =
                discountMap[discountCode]?.let { getStringBlocking(it) }
                    ?: getStringBlocking(Res.string.zolotaya_korona_unknown, discountCode.toString(16))
            val cardTypeName =
                CARD_NAMES[cardType]?.let { getStringBlocking(it) }
                    ?: cardType.toString(16)
            return listOf(
                ListItem(Res.string.zolotaya_korona_region, regionName),
                ListItem(Res.string.zolotaya_korona_card_type, cardTypeName),
                ListItem(Res.string.zolotaya_korona_discount, discountName),
                ListItem(Res.string.zolotaya_korona_card_serial, cardSerial.uppercase()),
                ListItem(Res.string.zolotaya_korona_refill_counter, refill?.counter?.toString() ?: "0"),
            )
        }

    override val trips: List<Trip>
        get() = listOfNotNull(trip) + listOfNotNull(refill)
}
