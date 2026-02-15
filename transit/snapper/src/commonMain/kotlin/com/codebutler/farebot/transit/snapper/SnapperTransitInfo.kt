/*
 * SnapperTransitInfo.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
 *
 * Reference: https://github.com/micolous/metrodroid/wiki/Snapper
 */

package com.codebutler.farebot.transit.snapper

import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.card.ksx6924.KSX6924Application
import com.codebutler.farebot.card.ksx6924.KSX6924PurseInfo
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.snapper.generated.resources.Res
import farebot.transit.snapper.generated.resources.snapper_card_name
import kotlinx.datetime.TimeZone
import com.codebutler.farebot.base.util.FormattedString

class SnapperTransitInfo internal constructor(
    private val mBalance: Int,
    private val mPurseInfo: KSX6924PurseInfo?,
    private val mTrips: List<Trip>,
    private val mSerialNumber: String?,
) : TransitInfo() {
    override val serialNumber: String?
        get() = mPurseInfo?.serial ?: mSerialNumber

    override val balance: TransitBalance?
        get() =
            mPurseInfo?.buildTransitBalance(
                balance = TransitCurrency.NZD(mBalance),
                tz = TZ,
            ) ?: if (mBalance != 0) {
                TransitBalance(TransitCurrency.NZD(mBalance))
            } else {
                null
            }

    override val cardName: FormattedString
        get() = getCardName()

    override val info: List<ListItemInterface>?
        get() = mPurseInfo?.getInfo(SnapperPurseInfoResolver)

    override val trips: List<Trip>
        get() = mTrips

    companion object {
        private val TZ = TimeZone.of("Pacific/Auckland")

        fun getCardName(): FormattedString = FormattedString(Res.string.snapper_card_name)

        fun create(card: KSX6924Application): SnapperTransitInfo {
            val purseInfo = card.purseInfo
            val balance = card.balance.byteArrayToInt()

            val trips =
                TransactionTrip.merge(
                    getSnapperTransactionRecords(card).map {
                        SnapperTransaction.parseTransaction(it.first, it.second)
                    },
                )

            return SnapperTransitInfo(
                mBalance = balance,
                mPurseInfo = purseInfo,
                mTrips = trips,
                mSerialNumber = card.serial,
            )
        }

        fun createEmpty(serialNumber: String? = null): SnapperTransitInfo =
            SnapperTransitInfo(
                mBalance = 0,
                mPurseInfo = null,
                mTrips = emptyList(),
                mSerialNumber = serialNumber,
            )

        private fun getSnapperTransactionRecords(card: KSX6924Application): List<Pair<ByteArray, ByteArray>> {
            val trips = card.application.getSfiFile(3) ?: return emptyList()
            val balances = card.application.getSfiFile(4) ?: return emptyList()

            return trips.recordList zip balances.recordList
        }
    }
}
