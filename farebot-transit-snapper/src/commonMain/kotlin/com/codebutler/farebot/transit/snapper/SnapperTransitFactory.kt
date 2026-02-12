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

import com.codebutler.farebot.base.util.isAllFF
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.iso7816.ISO7816Card
import com.codebutler.farebot.card.ksx6924.KSX6924Application
import com.codebutler.farebot.card.ksx6924.KSX6924CardTransitFactory
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_snapper.generated.resources.*

/**
 * Transit factory for Snapper cards (Wellington, New Zealand).
 *
 * Snapper uses the KSX6924 (T-Money compatible) protocol over ISO 7816.
 * Ported from Metrodroid.
 */
class SnapperTransitFactory : TransitFactory<ISO7816Card, SnapperTransitInfo>,
    KSX6924CardTransitFactory {

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)

    // ========================================================================
    // TransitFactory<ISO7816Card, SnapperTransitInfo> implementation
    // ========================================================================

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
        val ksx6924App = extractKSX6924Application(card)
            ?: return TransitIdentity.create(SnapperTransitInfo.getCardName(), null)
        return parseTransitIdentity(ksx6924App)
    }

    override fun parseInfo(card: ISO7816Card): SnapperTransitInfo {
        val ksx6924App = extractKSX6924Application(card)
            ?: return SnapperTransitInfo.createEmpty()
        return parseTransitData(ksx6924App)
    }

    // ========================================================================
    // KSX6924CardTransitFactory implementation
    // ========================================================================

    override fun check(app: KSX6924Application): Boolean {
        // Snapper cards have SFI 4 records where bytes 26..46 are all 0xFF
        val sfiFile = app.application.getSfiFile(4) ?: return false
        return sfiFile.recordList.all { record ->
            record.size == 46 && record.copyOfRange(26, 46).isAllFF()
        }
    }

    override fun parseTransitIdentity(app: KSX6924Application): TransitIdentity {
        return TransitIdentity.create(SnapperTransitInfo.getCardName(), app.serial)
    }

    override fun parseTransitData(app: KSX6924Application): SnapperTransitInfo {
        return SnapperTransitInfo.create(app)
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

        // Extract balance data stored by ISO7816CardReader with "balance/0" key
        val balanceData = app.getFile("balance/0")?.binaryData ?: ByteArray(4) { 0 }

        // Extract extra records stored with "extra/N" keys
        val extraRecords = (0..0xf).mapNotNull { i ->
            app.getFile("extra/$i")?.binaryData
        }

        return KSX6924Application(
            application = app,
            balance = balanceData,
            extraRecords = extraRecords
        )
    }

    companion object {
        private val CARD_INFO = CardInfo(
            nameRes = Res.string.card_name_snapper,
            cardType = CardType.ISO7816,
            region = TransitRegion.NEW_ZEALAND,
            locationRes = Res.string.card_location_wellington_new_zealand,
            imageRes = Res.drawable.snapperplus,
            latitude = -41.2865f,
            longitude = 174.7762f,
            brandColor = 0xD52726,
        )

        /**
         * KSX6924-compatible application AIDs.
         */
        private val KSX6924_AIDS = listOf(
            "d4100000030001",
            "d4100000140001",
            "d4100000300001",
            "d4106509900020"
        )
    }
}
