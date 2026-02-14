/*
 * TMoneyTransitInfo.kt
 *
 * Copyright 2018 Google
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.tmoney

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.ksx6924.KSX6924Application
import com.codebutler.farebot.card.ksx6924.KSX6924PurseInfo
import com.codebutler.farebot.card.ksx6924.KSX6924PurseInfoResolver
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.tmoney.generated.resources.Res
import farebot.transit.tmoney.generated.resources.card_name_tmoney
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable

/**
 * Transit data for T-Money cards (South Korea).
 *
 * T-Money is a contactless smart card used for public transit and small purchases
 * throughout South Korea. It uses the KSX6924 protocol standard.
 *
 * See https://github.com/micolous/metrodroid/wiki/T-Money for more information.
 */
@Serializable
open class TMoneyTransitInfo protected constructor(
    protected val mBalance: Int,
    protected val mPurseInfo: KSX6924PurseInfo?,
    private val mTrips: List<TMoneyTrip>,
    private val mSerialNumber: String?,
) : TransitInfo() {
    override val serialNumber: String?
        get() = mPurseInfo?.serial ?: mSerialNumber

    override val balance: TransitBalance?
        get() =
            mPurseInfo?.buildTransitBalance(
                balance = TransitCurrency.KRW(mBalance),
                tz = TZ,
            ) ?: if (mBalance != 0) {
                TransitBalance(TransitCurrency.KRW(mBalance))
            } else {
                null
            }

    override val cardName: String
        get() = getCardName()

    override val info: List<ListItemInterface>?
        get() = mPurseInfo?.getInfo(purseInfoResolver)

    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree? =
        mPurseInfo?.getAdvancedInfo(stringResource, purseInfoResolver)

    override val trips: List<Trip>
        get() = mTrips

    /**
     * Returns the purse info resolver for this card type.
     * Subclasses can override this to provide their own resolver.
     */
    protected open val purseInfoResolver: KSX6924PurseInfoResolver
        get() = TMoneyPurseInfoResolver

    companion object {
        private val TZ = TimeZone.of("Asia/Seoul")

        fun getCardName(): String = getStringBlocking(Res.string.card_name_tmoney)

        /**
         * Creates a [TMoneyTransitInfo] from a [KSX6924Application].
         */
        fun create(card: KSX6924Application): TMoneyTransitInfo? {
            val purseInfo = card.purseInfo ?: return null

            val balance = card.balance.byteArrayToInt()

            val trips =
                card.transactionRecords
                    ?.mapNotNull { record ->
                        TMoneyTrip.parseTrip(record)
                    }.orEmpty()

            return TMoneyTransitInfo(
                mBalance = balance,
                mPurseInfo = purseInfo,
                mTrips = trips,
                mSerialNumber = null,
            )
        }

        /**
         * Creates an empty [TMoneyTransitInfo] for when full card data is not available.
         */
        fun createEmpty(serialNumber: String? = null): TMoneyTransitInfo =
            TMoneyTransitInfo(
                mBalance = 0,
                mPurseInfo = null,
                mTrips = emptyList(),
                mSerialNumber = serialNumber,
            )
    }
}
