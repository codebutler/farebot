/*
 * SunCardTransitFactory.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_serialonly.generated.resources.*

class SunCardTransitFactory : TransitFactory<ClassicCard, SunCardTransitInfo> {

    override val allCards: List<CardInfo> = listOf(
        CardInfo(
            nameRes = Res.string.card_name_sun_card,
            cardType = CardType.MifareClassic,
            region = TransitRegion.USA,
            locationRes = Res.string.card_location_orlando_fl,
            serialOnly = true,
            imageRes = Res.drawable.suncard,
            latitude = 28.5383f,
            longitude = -81.3792f,
            brandColor = 0xFDC448,
        )
    )

    companion object {
        internal const val NAME = "SunRail SunCard"

        internal fun getSerial(card: ClassicCard): Int =
            (card.getSector(0) as DataClassicSector).getBlock(1).data.byteArrayToInt(3, 4)

        internal fun formatSerial(serial: Int): String = serial.toString()
        internal fun formatLongSerial(serial: Int): String = "637426" + NumberUtils.zeroPad(serial, 10)
        internal fun formatBarcodeSerial(serial: Int): String =
            "799366314176000637426" + NumberUtils.zeroPad(serial, 10)
    }

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false
        return sector0.getBlock(1).data.byteArrayToInt(7, 4) == 0x070515ff
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity =
        TransitIdentity.create(NAME, formatSerial(getSerial(card)))

    override fun parseInfo(card: ClassicCard): SunCardTransitInfo =
        SunCardTransitInfo(mSerial = getSerial(card))
}
