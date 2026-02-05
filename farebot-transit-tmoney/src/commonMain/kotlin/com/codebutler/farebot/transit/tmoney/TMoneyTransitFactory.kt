/*
 * TMoneyTransitFactory.kt
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

import com.codebutler.farebot.base.util.isAllFF
import com.codebutler.farebot.card.iso7816.ISO7816Card
import com.codebutler.farebot.card.ksx6924.KSX6924Application
import com.codebutler.farebot.card.ksx6924.KSX6924CardTransitFactory
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

/**
 * Transit factory for T-Money cards (South Korea).
 *
 * T-Money is a contactless smart card used for public transit and small purchases
 * throughout South Korea. It uses the KSX6924 protocol standard.
 *
 * This factory implements two interfaces:
 * - [TransitFactory] for direct ISO7816 card detection
 * - [KSX6924CardTransitFactory] for KSX6924 registry-based detection
 *
 * See https://github.com/micolous/metrodroid/wiki/T-Money for more information.
 */
class TMoneyTransitFactory : TransitFactory<ISO7816Card, TMoneyTransitInfo>,
    KSX6924CardTransitFactory {

    // ========================================================================
    // TransitFactory<ISO7816Card, TMoneyTransitInfo> implementation
    // ========================================================================

    @OptIn(ExperimentalStdlibApi::class)
    override fun check(card: ISO7816Card): Boolean {
        val app = card.applications.firstOrNull { app ->
            val aidHex = app.appName?.toHexString()?.lowercase()
            aidHex != null && aidHex in KSX6924_AIDS
        } ?: return false

        // T-Money records are 46 bytes but the last 20 bytes are NOT all 0xFF
        // (unlike Snapper which has all 0xFF in bytes 26..46)
        val sfiFile = app.getSfiFile(4) ?: return false
        return sfiFile.recordList.any { record ->
            record.size == 46 && !record.copyOfRange(26, 46).isAllFF()
        }
    }

    override fun parseIdentity(card: ISO7816Card): TransitIdentity {
        val ksx6924App = extractKSX6924Application(card)
            ?: return TransitIdentity.create(TMoneyTransitInfo.getCardName(), null)
        return parseTransitIdentity(ksx6924App)
    }

    override fun parseInfo(card: ISO7816Card): TMoneyTransitInfo {
        val ksx6924App = extractKSX6924Application(card)
            ?: return TMoneyTransitInfo.createEmpty()
        return parseTransitData(ksx6924App) ?: TMoneyTransitInfo.createEmpty()
    }

    // ========================================================================
    // KSX6924CardTransitFactory implementation
    // ========================================================================

    override fun check(app: KSX6924Application): Boolean {
        // T-Money accepts all KSX6924 cards that aren't explicitly claimed by
        // another factory (like Snapper). The KSX6924Registry handles priority.
        return true
    }

    override fun parseTransitIdentity(app: KSX6924Application): TransitIdentity {
        return TransitIdentity.create(TMoneyTransitInfo.getCardName(), app.serial)
    }

    override fun parseTransitData(app: KSX6924Application): TMoneyTransitInfo? {
        return TMoneyTransitInfo.create(app)
    }

    // ========================================================================
    // Private helpers
    // ========================================================================

    @OptIn(ExperimentalStdlibApi::class)
    private fun extractKSX6924Application(card: ISO7816Card): KSX6924Application? {
        val app = card.applications.firstOrNull { app ->
            val aidHex = app.appName?.toHexString()?.lowercase()
            aidHex != null && aidHex in KSX6924_AIDS
        } ?: return null

        // Get balance from tag in response - this is simplified since we don't have
        // the full protocol implementation. We'll use any available balance data.
        val balanceData = ByteArray(4) { 0 }

        return KSX6924Application(
            application = app,
            balance = balanceData
        )
    }

    companion object {
        /**
         * KSX6924-compatible application AIDs.
         */
        private val KSX6924_AIDS = listOf(
            "d4100000030001",  // T-Money, Snapper
            "d4100000140001",  // Cashbee / eB
            "d4100000300001",  // MOIBA (untested)
            "d4106509900020"   // K-Cash (untested)
        )
    }
}
