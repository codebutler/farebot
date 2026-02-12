/*
 * TPFCardTransitFactory.kt
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

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.base.util.reverseBuffer
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_serialonly.generated.resources.*

class TPFCardTransitFactory : TransitFactory<DesfireCard, TPFCardTransitInfo> {

    override val allCards: List<CardInfo> = listOf(
        CardInfo(
            nameRes = Res.string.card_name_tpf,
            cardType = CardType.MifareDesfire,
            region = TransitRegion.SWITZERLAND,
            locationRes = Res.string.card_location_fribourg_switzerland,
            serialOnly = true,
            imageRes = Res.drawable.tpf_card,
            latitude = 46.8065f,
            longitude = 7.1620f,
            brandColor = 0xA01D3D,
            credits = listOf("Metrodroid Project"),
        )
    )

    override fun check(card: DesfireCard): Boolean =
        card.getApplication(APP_ID) != null

    override fun parseIdentity(card: DesfireCard): TransitIdentity =
        TransitIdentity.create(TPFCardTransitInfo.NAME, formatSerial(card))

    override fun parseInfo(card: DesfireCard): TPFCardTransitInfo =
        TPFCardTransitInfo(mSerial = formatSerial(card))

    companion object {
        // "CTK" in ASCII
        private const val APP_ID = 0x43544b

        internal fun formatSerial(card: DesfireCard): String =
            ByteUtils.getHexString(card.tagId.reverseBuffer()).uppercase()
    }
}
