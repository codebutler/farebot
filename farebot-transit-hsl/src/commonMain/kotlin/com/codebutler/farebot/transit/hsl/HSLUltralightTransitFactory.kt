/*
 * HSLUltralightTransitFactory.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.hsl

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.ultralight.UltralightCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransactionTrip
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_hsl.generated.resources.*
import com.codebutler.farebot.base.util.getStringBlocking

private fun getNameUL(city: Int) =
    if (city == HSLLookup.CITY_UL_TAMPERE) getStringBlocking(Res.string.tampere_ultralight_card_name)
    else getStringBlocking(Res.string.hsl_ultralight_card_name)

/**
 * HSL (Helsinki) and Tampere Ultralight transit cards.
 * Ported from Metrodroid.
 */
class HSLUltralightTransitFactory : TransitFactory<UltralightCard, HSLUltralightTransitInfo> {

    override val allCards: List<CardInfo> = emptyList()

    override fun check(card: UltralightCard): Boolean {
        val page4 = card.getPage(4).data
        return page4.getBitsFromBuffer(0, 4) in 1..2 &&
            page4.getBitsFromBuffer(8, 24) == 0x924621
    }

    override fun parseIdentity(card: UltralightCard): TransitIdentity {
        val city = card.pages[5].data.getBitsFromBuffer(0, 8)
        return TransitIdentity.create(
            getNameUL(city),
            formatSerial(getSerial(card))
        )
    }

    override fun parseInfo(card: UltralightCard): HSLUltralightTransitInfo {
        val raw = card.readPages(4, 12)
        val version = raw.getBitsFromBuffer(0, 4)
        val city = card.pages[5].data.getBitsFromBuffer(0, 8)

        val arvo = HSLArvo.parseUL(raw.sliceOffLen(7, 41), version, city)

        return HSLUltralightTransitInfo(
            serialNumber = formatSerial(getSerial(card)),
            subscriptions = listOfNotNull(arvo),
            applicationVersion = version,
            applicationKeyVersion = card.pages[4].data.getBitsFromBuffer(4, 4),
            platformType = card.pages[5].data.getBitsFromBuffer(20, 3),
            securityLevel = card.pages[5].data.getBitsFromBuffer(23, 1),
            trips = TransactionTrip.merge(listOfNotNull(arvo?.lastTransaction)),
            city = city
        )
    }

    companion object {
        private fun getSerial(card: UltralightCard): String {
            val num = (card.tagId.byteArrayToInt(1, 3) xor card.tagId.byteArrayToInt(4, 3)) and 0x7fffff
            return card.readPages(4, 2).getHexString(1, 5) +
                NumberUtils.zeroPad(num, 7) + card.pages[5].data.getBitsFromBuffer(16, 4)
        }

        internal fun formatSerial(serial: String): String {
            if (serial.length < 18) return serial
            return serial.substring(0, 6) + " " +
                serial.substring(6, 13) + " " +
                serial.substring(13, 17) + " " +
                serial.substring(17)
        }
    }
}

class HSLUltralightTransitInfo internal constructor(
    override val serialNumber: String,
    override val trips: List<Trip>,
    override val subscriptions: List<Subscription>?,
    val applicationVersion: Int,
    val applicationKeyVersion: Int,
    val platformType: Int,
    val securityLevel: Int,
    val city: Int
) : TransitInfo() {
    override val cardName: String get() = getNameUL(city)

    override val info: List<ListItemInterface>
        get() = listOf(
            ListItem(Res.string.hsl_application_version, applicationVersion.toString()),
            ListItem(Res.string.hsl_application_key_version, applicationKeyVersion.toString()),
            ListItem(Res.string.hsl_platform_type, platformType.toString()),
            ListItem(Res.string.hsl_security_level, securityLevel.toString())
        )
}
