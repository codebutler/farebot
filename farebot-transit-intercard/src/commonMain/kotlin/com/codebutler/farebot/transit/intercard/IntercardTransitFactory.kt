/*
 * IntercardTransitFactory.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.intercard

import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.byteArrayToLongReversed
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.card.desfire.ValueDesfireFileSettings
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

class IntercardTransitFactory : TransitFactory<DesfireCard, IntercardTransitInfo> {

    override fun check(card: DesfireCard): Boolean {
        return card.getApplication(APP_ID_BALANCE) != null
    }

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        return TransitIdentity.create(
            IntercardTransitInfo.NAME,
            card.tagId.byteArrayToLongReversed().toString()
        )
    }

    override fun parseInfo(card: DesfireCard): IntercardTransitInfo {
        val file1 = card.getApplication(APP_ID_BALANCE)?.getFile(1)
        val balance = (file1 as? StandardDesfireFile)?.data?.byteArrayToIntReversed()
        val lastTransaction = (file1?.fileSettings as? ValueDesfireFileSettings)?.limitedCreditValue
        return IntercardTransitInfo(
            mBalance = balance,
            mLastTransaction = lastTransaction,
            mSerialNumber = card.tagId.byteArrayToLongReversed()
        )
    }

    companion object {
        const val APP_ID_BALANCE = 0x5f8415
    }
}
