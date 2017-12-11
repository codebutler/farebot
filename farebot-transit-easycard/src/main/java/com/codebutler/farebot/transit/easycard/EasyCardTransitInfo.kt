/*
 * EasyCardTransitInfo.kt
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Based on code from http://www.fuzzysecurity.com/tutorials/rfid/4.html
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
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

package com.codebutler.farebot.transit.easycard

import android.content.Context
import android.content.res.Resources
import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.ui.uiTree
import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import java.text.NumberFormat
import java.util.Currency
import java.util.Date

data class EasyCardTransitInfo(
        private val serialNumber: String,
        private val manufacturingDate: Date,
        private val balance: Long,
        private val trips: List<Trip>,
        private val refill: Refill) : TransitInfo() {

    override fun getCardName(resources: Resources): String =
            resources.getString(R.string.easycard_card_name)

    override fun getSerialNumber(): String? = serialNumber

    override fun getBalanceString(resources: Resources): String {
        val numberFormat = NumberFormat.getCurrencyInstance()
        numberFormat.currency = Currency.getInstance("TWD")
        return numberFormat.format(balance)
    }

    override fun getTrips(): List<Trip> = trips

    override fun getRefills(): List<Refill> = listOf(refill)

    override fun getSubscriptions(): List<Subscription> = listOf()

    override fun getAdvancedUi(context: Context): FareBotUiTree? = uiTree(context) {
        item {
            title = R.string.easycard_manufactoring_date
            value = manufacturingDate
        }
    }
}
