/*
 * EasyCardTransitFactory.kt
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
import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.Refill
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.Trip
import com.codebutler.farebot.transit.registry.annotations.TransitCard
import java.text.NumberFormat
import java.util.Currency
import java.util.Date

@TransitCard
class EasyCardTransitFactory(private val context: Context) : TransitFactory<ClassicCard, EasyCardTransitInfo> {

    override fun check(card: ClassicCard): Boolean {
        val data = (card.getSector(0) as? DataClassicSector)?.getBlock(0)?.data?.bytes()
        return data != null && data[6] == 0x4.toByte() && data[7] == 0x0.toByte() && data[8] == 0x46.toByte()
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity {
        val uid = parseSerialNumber(card)
        return TransitIdentity.create(context.getString(R.string.easycard_card_name), uid)
    }

    override fun parseInfo(card: ClassicCard): EasyCardTransitInfo {
        return EasyCardTransitInfo(
                parseSerialNumber(card),
                parseManufacturingDate(card),
                parseBalance(card),
                parseTrips(card),
                parseRefill(card))
    }

    private fun parseSerialNumber(card: ClassicCard): String {
        val data = (card.getSector(0) as? DataClassicSector)?.getBlock(0)?.data?.bytes()!!
        return ByteUtils.getHexString(data.copyOfRange(0, 4))
    }

    private fun parseBalance(card: ClassicCard): Long {
        val data = (card.getSector(2) as? DataClassicSector)?.getBlock(0)?.data?.bytes()!!
        return ByteUtils.byteArrayToLong(data, 0, 1)
    }

    private fun parseRefill(card: ClassicCard): Refill {
        val data = (card.getSector(2) as? DataClassicSector)?.getBlock(2)?.data?.bytes()!!

        val location = EasyCardStations[data[11].toInt()] ?: context.getString(R.string.easycard_unknown)
        val date = ByteUtils.byteArrayToLong(data.copyOfRange(1, 5).reversedArray())
        val amount = data[6].toLong()

        return EasyCardRefill(date, location, amount)
    }

    private fun parseTrips(card: ClassicCard): List<Trip> {
        val blocks = (
                (card.getSector(3) as DataClassicSector).blocks.subList(1, 3) +
                (card.getSector(4) as DataClassicSector).blocks.subList(0, 3) +
                (card.getSector(5) as DataClassicSector).blocks.subList(0, 3))
                .filter { !it.data.bytes().all { it == 0x0.toByte() } }

        return blocks.map { block ->
            val data = block.data.bytes()
            val timestamp = ByteUtils.byteArrayToLong(data.copyOfRange(1, 5).reversedArray())
            val fare = data[6].toLong()
            val balance = data[8].toLong()
            val transactionType = data[11].toInt()
            EasyCardTrip(timestamp, fare, balance, transactionType)
        }.distinctBy { it.timestamp }
    }

    private fun parseManufacturingDate(card: ClassicCard): Date {
        val data = (card.getSector(0) as? DataClassicSector)?.getBlock(0)?.data?.bytes()!!
        return Date(ByteUtils.byteArrayToLong(data.copyOfRange(5, 9).reversedArray()) * 1000L)
    }

    private data class EasyCardRefill(
        private val timestamp: Long,
        private val location: String,
        private val amount: Long
    ) : Refill() {

        override fun getTimestamp(): Long = timestamp

        override fun getAgencyName(resources: Resources): String = location

        override fun getShortAgencyName(resources: Resources): String = location

        override fun getAmount(): Long = amount

        override fun getAmountString(resources: Resources): String {
            val numberFormat = NumberFormat.getCurrencyInstance()
            numberFormat.currency = Currency.getInstance("TWD")
            return numberFormat.format(amount)
        }
    }

    private data class EasyCardTrip(
        private val timestamp: Long,
        private val fare: Long,
        private val balance: Long,
        private val transactionType: Int
    ) : Trip() {

        override fun getTimestamp(): Long = timestamp

        override fun getExitTimestamp(): Long = timestamp

        override fun getRouteName(resources: Resources): String? = EasyCardStations[transactionType]

        override fun getAgencyName(resources: Resources): String? = null

        override fun getShortAgencyName(resources: Resources): String? = null

        override fun getBalanceString(): String? {
            val numberFormat = NumberFormat.getCurrencyInstance()
            numberFormat.currency = Currency.getInstance("TWD")
            return numberFormat.format(balance)
        }

        override fun getStartStationName(resources: Resources): String? = null

        override fun getStartStation(): Station? = null

        override fun getEndStationName(resources: Resources): String? = null

        override fun getEndStation(): Station? = null

        override fun hasFare(): Boolean = true

        override fun getFareString(resources: Resources): String? {
            val numberFormat = NumberFormat.getCurrencyInstance()
            numberFormat.currency = Currency.getInstance("TWD")
            return numberFormat.format(fare)
        }

        override fun getMode(): Mode? = when (transactionType) {
            0x05 -> Mode.BUS
            0x01 -> Mode.POS
            else -> Mode.TRAIN
        }

        override fun hasTime(): Boolean = true
    }
}
