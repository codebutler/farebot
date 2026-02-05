/*
 * SnapperTransitFactory.kt
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

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.base.util.isAllFF
import com.codebutler.farebot.card.iso7816.ISO7816Card
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

/**
 * Transit factory for Snapper cards (Wellington, New Zealand).
 *
 * Snapper uses the KSX6924 (T-Money compatible) protocol over ISO 7816.
 * This implementation identifies the card by its application AID and provides
 * basic card identification. Full balance and trip parsing requires the KSX6924
 * protocol which is not yet available in FareBot.
 *
 * Ported from Metrodroid.
 */
class SnapperTransitFactory : TransitFactory<ISO7816Card, SnapperTransitInfo> {

    @OptIn(ExperimentalStdlibApi::class)
    override fun check(card: ISO7816Card): Boolean {
        val app = card.applications.firstOrNull { app ->
            val aidHex = app.appName?.toHexString()?.lowercase()
            aidHex != null && aidHex in KSX6924_AIDS
        } ?: return false

        // Snapper cards have a slightly different record format from other KSX6924 cards:
        // SFI 4 records are 46 bytes and bytes 26..46 are all 0xFF.
        val sfiFile = app.getSfiFile(4) ?: return false
        return sfiFile.recordList.all { record ->
            record.size == 46 && record.copyOfRange(26, 46).isAllFF()
        }
    }

    override fun parseIdentity(card: ISO7816Card): TransitIdentity {
        return TransitIdentity.create(SnapperTransitInfo.getCardName(), null)
    }

    override fun parseInfo(card: ISO7816Card): SnapperTransitInfo {
        return SnapperTransitInfo()
    }

    companion object {
        /**
         * KSX6924-compatible application AIDs.
         * Snapper uses the T-Money AID: D4100000030001
         */
        private val KSX6924_AIDS = listOf(
            "d4100000030001",
            "d4100000140001",
            "d4100000300001",
            "d4106509900020"
        )
    }
}
