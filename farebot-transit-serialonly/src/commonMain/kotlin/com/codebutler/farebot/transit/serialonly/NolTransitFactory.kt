/*
 * NolTransitFactory.kt
 *
 * Copyright 2019 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_serialonly.generated.resources.*

class NolTransitFactory : TransitFactory<DesfireCard, NolTransitInfo> {
    override val allCards: List<CardInfo> =
        listOf(
            CardInfo(
                nameRes = Res.string.card_name_nol,
                cardType = CardType.MifareDesfire,
                region = TransitRegion.UAE,
                locationRes = Res.string.card_location_dubai_uae,
                serialOnly = true,
                imageRes = Res.drawable.nol,
                latitude = 25.2048f,
                longitude = 55.2708f,
                brandColor = null,
                credits = listOf("Metrodroid Project"),
            ),
        )

    companion object {
        private const val APP_ID_SERIAL = 0xffffff
        internal const val NAME = "Nol"

        internal fun getSerial(card: DesfireCard): Int? {
            val file =
                card.getApplication(APP_ID_SERIAL)?.getFile(8) as? StandardDesfireFile
                    ?: return null
            return file.data.getBitsFromBuffer(61, 32)
        }

        internal fun formatSerial(serial: Int?): String? =
            if (serial != null) {
                NumberUtils.formatNumber(serial.toLong(), " ", 3, 3, 4)
            } else {
                null
            }
    }

    override fun check(card: DesfireCard): Boolean =
        card.getApplication(0x4078) != null && card.getApplication(APP_ID_SERIAL) != null

    override fun parseIdentity(card: DesfireCard): TransitIdentity =
        TransitIdentity.create(NAME, formatSerial(getSerial(card)))

    override fun parseInfo(card: DesfireCard): NolTransitInfo {
        val serial = getSerial(card)
        val type =
            (card.getApplication(APP_ID_SERIAL)?.getFile(8) as? StandardDesfireFile)
                ?.data
                ?.byteArrayToInt(0xc, 2)
        return NolTransitInfo(mSerial = serial, mType = type)
    }
}
