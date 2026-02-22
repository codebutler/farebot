/*
 * IstanbulKartTransitFactory.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.transit.serialonly.generated.resources.*

class IstanbulKartTransitFactory : TransitFactory<DesfireCard, IstanbulKartTransitInfo> {
    override val allCards: List<CardInfo> =
        listOf(
            CardInfo(
                nameRes = Res.string.card_name_istanbul_kart,
                cardType = CardType.MifareDesfire,
                region = TransitRegion.TURKEY,
                locationRes = Res.string.card_location_istanbul_turkey,
                serialOnly = true,
                imageRes = Res.drawable.istanbulkart_card,
                latitude = 41.0082f,
                longitude = 28.9784f,
                brandColor = 0xA7C5FD,
                credits = listOf("Metrodroid Project", "Vladimir Serbinenko", "Michael Farrell"),
            ),
        )

    companion object {
        internal const val APP_ID = 0x422201
        internal val NAME = FormattedString("IstanbulKart")

        internal fun parseSerial(card: DesfireCard): String? =
            (card.getApplication(APP_ID)?.getFile(2) as? StandardDesfireFile)
                ?.data
                ?.getHexString(0, 8)

        internal fun formatSerial(serial: String): String = NumberUtils.groupString(serial, " ", 4, 4, 4)
    }

    override fun check(card: DesfireCard): Boolean = card.getApplication(APP_ID) != null

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        val serial = parseSerial(card)
        return TransitIdentity.create(NAME, serial?.let { formatSerial(it) })
    }

    override fun parseInfo(card: DesfireCard): IstanbulKartTransitInfo {
        val serial = parseSerial(card) ?: ""
        val serial2 = card.tagId.hex()
        return IstanbulKartTransitInfo(mSerial = serial, mSerial2 = serial2)
    }
}
