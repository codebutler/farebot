/*
 * KROCAPTransitFactory.kt
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
 */

package com.codebutler.farebot.transit.krocap

import com.codebutler.farebot.card.iso7816.ISO7816Card
import com.codebutler.farebot.card.iso7816.ISO7816TLV
import com.codebutler.farebot.card.ksx6924.KROCAPData
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

/**
 * Transit factory for KR-OCAP (One Card All Pass) cards from South Korea.
 *
 * KR-OCAP is a Korean transit card standard. This factory handles cards
 * that have the KR-OCAP Config DF application but do NOT have a KSX6924
 * application. If a KSX6924 application is present, the T-Money parser
 * should be used instead.
 *
 * Reference: https://github.com/micolous/metrodroid/wiki/South-Korea#a0000004520001
 */
class KROCAPTransitFactory : TransitFactory<ISO7816Card, KROCAPTransitInfo> {

    override fun check(card: ISO7816Card): Boolean {
        // Only handle cards with KR-OCAP Config DF but WITHOUT KSX6924 application.
        // If KSX6924 is present, defer to T-Money/Snapper handlers.
        if (hasKSX6924Application(card)) {
            return false
        }

        return getKROCAPConfigDFApplication(card) != null
    }

    override fun parseIdentity(card: ISO7816Card): TransitIdentity {
        val app = getKROCAPConfigDFApplication(card)
            ?: throw IllegalArgumentException("Not a KR-OCAP card")
        val pdata = app.appProprietaryBerTlv
            ?: throw IllegalArgumentException("Missing FCI data")
        val serial = getSerial(pdata)
        return TransitIdentity.create(KROCAPTransitInfo.NAME, serial)
    }

    override fun parseInfo(card: ISO7816Card): KROCAPTransitInfo {
        val app = getKROCAPConfigDFApplication(card)
            ?: throw IllegalArgumentException("Not a KR-OCAP card")
        val pdata = app.appProprietaryBerTlv
            ?: throw IllegalArgumentException("Missing FCI data")
        return KROCAPTransitInfo(pdata)
    }

    companion object {
        /**
         * KR-OCAP Config DF AID: A0000004520001
         */
        @OptIn(ExperimentalStdlibApi::class)
        private val KROCAP_CONFIG_DF_AID = "a0000004520001".hexToByteArray()

        /**
         * KSX6924-compatible application AIDs.
         */
        @OptIn(ExperimentalStdlibApi::class)
        private val KSX6924_AIDS = listOf(
            "d4100000030001".hexToByteArray(),  // T-Money, Snapper
            "d4100000140001".hexToByteArray(),  // Cashbee / eB
            "d4100000300001".hexToByteArray(),  // MOIBA
            "d4106509900020".hexToByteArray()   // K-Cash
        )

        private fun hasKSX6924Application(card: ISO7816Card): Boolean {
            return card.applications.any { app ->
                val appName = app.appName ?: return@any false
                KSX6924_AIDS.any { it.contentEquals(appName) }
            }
        }

        private fun getKROCAPConfigDFApplication(card: ISO7816Card): com.codebutler.farebot.card.iso7816.ISO7816Application? {
            return card.applications.firstOrNull { app ->
                val appName = app.appName ?: return@firstOrNull false
                appName.contentEquals(KROCAP_CONFIG_DF_AID)
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        private fun getSerial(pdata: ByteArray): String? {
            val tagBytes = KROCAPData.TAG_SERIAL_NUMBER.hexToByteArray()
            return ISO7816TLV.findBERTLV(pdata, tagBytes, false)?.toHexString()
        }
    }
}
