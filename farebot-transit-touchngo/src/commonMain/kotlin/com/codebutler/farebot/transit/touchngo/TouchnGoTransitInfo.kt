/*
 * TouchnGoTransitInfo.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
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

package com.codebutler.farebot.transit.touchngo

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_touchngo.generated.resources.Res
import farebot.farebot_transit_touchngo.generated.resources.touchngo_cardno
import farebot.farebot_transit_touchngo.generated.resources.touchngo_transaction_counter
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class TouchnGoTransitInfo(
    private val balanceValue: Int,
    private val serial: Long,
    private val txnCounter: Int,
    private val isTravelPass: Boolean,
    override val trips: List<Trip>,
    private val cardNo: Int,
    private val storedLuhn: Int,
    private val issueCounter: Int,
    private val issueDate: LocalDate,
    private val expiryDate: LocalDate,
) : TransitInfo() {

    override val cardName: String = TouchnGoTransitFactory.NAME

    override val serialNumber: String
        get() = NumberUtils.zeroPad(serial, 10)

    override val balance: TransitBalance
        get() {
            val issueInstant = localDateToInstant(issueDate)
            val expiryInstant = localDateToInstant(expiryDate)
            return TransitBalance(
                balance = TransitCurrency.MYR(balanceValue),
                name = null,
                validFrom = issueInstant,
                validTo = expiryInstant
            )
        }

    override val subscriptions: List<Subscription>?
        get() = if (isTravelPass) {
            listOf(TouchnGoTravelPass(localDateToInstant(issueDate)))
        } else {
            null
        }

    override val info: List<ListItemInterface>
        get() {
            val partialCardNo = "6014640" + NumberUtils.zeroPad(cardNo, 10)
            val cardNoFull = partialCardNo + calculateLuhn(partialCardNo)
            return listOf(
                ListItem(Res.string.touchngo_cardno, cardNoFull),
                ListItem(Res.string.touchngo_transaction_counter, txnCounter.toString())
            )
        }

    override val hasUnknownStations: Boolean = true

    companion object {
        /**
         * Calculates the Luhn check digit for the given number string.
         */
        internal fun calculateLuhn(number: String): Int {
            var sum = 0
            var alternate = true
            for (i in number.length - 1 downTo 0) {
                var n = number[i].digitToInt()
                if (alternate) {
                    n *= 2
                    if (n > 9) {
                        n -= 9
                    }
                }
                sum += n
                alternate = !alternate
            }
            return (10 - (sum % 10)) % 10
        }

        internal fun localDateToInstant(date: LocalDate): Instant {
            // Convert to start of day in UTC as a reasonable approximation
            val epochDays = date.toEpochDays()
            return Instant.fromEpochSeconds(epochDays * 86400L)
        }
    }
}
