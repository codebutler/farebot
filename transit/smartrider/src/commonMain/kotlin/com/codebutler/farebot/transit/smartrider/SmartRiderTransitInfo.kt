/*
 * SmartRiderTransitInfo.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.smartrider

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.ui.uiTree
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.smartrider.generated.resources.*

/**
 * Reader for SmartRider (Western Australia) and MyWay (Australian Capital Territory / Canberra).
 * https://github.com/micolous/metrodroid/wiki/SmartRider
 * https://github.com/micolous/metrodroid/wiki/MyWay
 */
class SmartRiderTransitInfo(
    private val serialNumberValue: String?,
    private val mBalance: Int,
    override val trips: List<Trip>,
    private val mSmartRiderType: SmartRiderType,
    private val mIssueDate: Int,
    private val mTokenType: Int,
    private val mTokenExpiryDate: Int,
    private val mAutoloadThreshold: Int,
    private val mAutoloadValue: Int,
) : TransitInfo() {
    override val cardName: FormattedString
        get() = FormattedString(mSmartRiderType.friendlyName)

    override val serialNumber: String?
        get() = serialNumberValue

    override val balance: TransitBalance
        get() {
            val aud = TransitCurrency.AUD(mBalance)
            val tokenType = localisedTokenType
            return when {
                mIssueDate > 0 && mTokenExpiryDate > 0 ->
                    TransitBalance(
                        balance = aud,
                        name = tokenType,
                        validFrom = convertDate(mIssueDate),
                        validTo = convertDate(mTokenExpiryDate),
                    )
                mIssueDate > 0 ->
                    TransitBalance(
                        balance = aud,
                        name = tokenType,
                        validFrom = convertDate(mIssueDate),
                    )
                mTokenExpiryDate > 0 ->
                    TransitBalance(
                        balance = aud,
                        name = tokenType,
                        validTo = convertDate(mTokenExpiryDate),
                    )
                else -> TransitBalance(balance = aud, name = tokenType)
            }
        }

    private val localisedTokenType: FormattedString?
        get() =
            when (mSmartRiderType) {
                SmartRiderType.SMARTRIDER ->
                    when (mTokenType) {
                        0x1 -> FormattedString(Res.string.smartrider_fare_standard)
                        0x2 -> FormattedString(Res.string.smartrider_fare_student)
                        0x4 -> FormattedString(Res.string.smartrider_fare_tertiary)
                        0x6 -> FormattedString(Res.string.smartrider_fare_senior)
                        0x7 -> FormattedString(Res.string.smartrider_fare_concession)
                        0xe -> FormattedString(Res.string.smartrider_fare_staff)
                        0xf -> FormattedString(Res.string.smartrider_fare_pensioner)
                        0x10 -> FormattedString(Res.string.smartrider_fare_convention)
                        else -> null
                    }
                else -> null
            }

    override val subscriptions: List<Subscription>? = null

    override suspend fun getAdvancedUi(): FareBotUiTree =
        uiTree {
            item {
                title = Res.string.smartrider_ticket_type
                value = mTokenType.toString()
            }
            if (mSmartRiderType == SmartRiderType.SMARTRIDER) {
                item {
                    title = Res.string.smartrider_autoload_threshold
                    value =
                        TransitCurrency.AUD(mAutoloadThreshold).formatCurrencyString(true)
                }
                item {
                    title = Res.string.smartrider_autoload_value
                    value =
                        TransitCurrency.AUD(mAutoloadValue).formatCurrencyString(true)
                }
            }
        }
}
