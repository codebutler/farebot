/*
 * NextfareDesfireTransitFactory.kt
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

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.base.util.Luhn
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFileSettings
import com.codebutler.farebot.card.desfire.UnauthorizedDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

class NextfareDesfireTransitFactory : TransitFactory<DesfireCard, NextfareDesfireTransitInfo> {

    override val allCards: List<CardInfo> = emptyList()

    override fun check(card: DesfireCard): Boolean {
        // Early check: exactly 1 app with ID 0x10000, app list not locked
        if (card.appListLocked || card.applications.size != 1 || card.applications[0].id != 0x10000)
            return false
        // Deep check: 1 file, file is unauthorized, file size is 384 bytes
        val app = card.getApplication(0x10000) ?: return false
        if (app.files.size != 1)
            return false
        val f = app.files[0] as? UnauthorizedDesfireFile ?: return false
        val fs = f.fileSettings as? StandardDesfireFileSettings ?: return false
        return fs.fileSize == 384
    }

    override fun parseIdentity(card: DesfireCard): TransitIdentity =
        TransitIdentity.create(NextfareDesfireTransitInfo.NAME, formatSerial(getSerial(card)))

    override fun parseInfo(card: DesfireCard): NextfareDesfireTransitInfo =
        NextfareDesfireTransitInfo(mSerial = getSerial(card))

    companion object {
        internal fun getSerial(card: DesfireCard): Long =
            card.tagId.byteArrayToLong(1, 6)

        internal fun formatSerial(serial: Long): String {
            var s = "0164" + NumberUtils.zeroPad(serial, 15)
            s += Luhn.calculateLuhn(s)
            return NumberUtils.groupString(s, " ", 4, 4, 4, 4, 4)
        }
    }
}
