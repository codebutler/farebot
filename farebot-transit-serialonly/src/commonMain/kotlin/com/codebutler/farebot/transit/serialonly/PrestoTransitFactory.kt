/*
 * PrestoTransitFactory.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.util.Luhn
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_serialonly.generated.resources.*

class PrestoTransitFactory : TransitFactory<DesfireCard, PrestoTransitInfo> {

    override val allCards: List<CardInfo> = listOf(
        CardInfo(
            nameRes = Res.string.card_name_presto,
            cardType = CardType.MifareDesfire,
            region = TransitRegion.CANADA,
            locationRes = Res.string.card_location_ontario_canada,
            serialOnly = true,
            imageRes = Res.drawable.presto_card,
            latitude = 43.6532f,
            longitude = -79.3832f,
            brandColor = 0x728452,
            credits = listOf("Metrodroid Project", "Vladimir Serbinenko", "Michael Farrell", "Philip Duncan"),
        )
    )

    companion object {
        private const val APP_ID_SERIAL = 0xff30ff
        internal const val NAME = "PRESTO"

        internal fun getSerial(card: DesfireCard): Int? {
            val file = card.getApplication(APP_ID_SERIAL)?.getFile(8) as? StandardDesfireFile
                ?: return null
            return file.data.getBitsFromBuffer(85, 24)
        }

        internal fun formatSerial(serial: Int?): String? {
            val s = serial ?: return null
            val main = "312401 ${NumberUtils.formatNumber(s.toLong(), " ", 4, 4)} 00"
            return main + Luhn.calculateLuhn(main.replace(" ", ""))
        }
    }

    override fun check(card: DesfireCard): Boolean =
        card.getApplication(0x2000) != null && card.getApplication(APP_ID_SERIAL) != null

    override fun parseIdentity(card: DesfireCard): TransitIdentity =
        TransitIdentity.create(NAME, formatSerial(getSerial(card)))

    override fun parseInfo(card: DesfireCard): PrestoTransitInfo =
        PrestoTransitInfo(mSerial = getSerial(card))
}
