/*
 * StrelkaTransitFactory.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_serialonly.generated.resources.*

class StrelkaTransitFactory : TransitFactory<ClassicCard, StrelkaTransitInfo> {

    override val allCards: List<CardInfo> = listOf(
        CardInfo(
            nameRes = Res.string.card_name_strelka,
            cardType = CardType.MifareClassic,
            region = TransitRegion.RUSSIA,
            locationRes = Res.string.card_location_moscow_region_russia,
            serialOnly = true,
            imageRes = Res.drawable.strelka_card,
            latitude = 55.7558f,
            longitude = 37.6173f,
            brandColor = 0x154477,
        )
    )

    companion object {
        internal const val NAME = "Strelka"

        internal fun getSerial(card: ClassicCard): String =
            (card.getSector(12) as DataClassicSector).getBlock(0).data
                .getHexString(2, 10).substring(0, 19)

        internal fun formatShortSerial(serial: String): String =
            NumberUtils.groupString(serial.substring(8), " ", 4, 4)
    }

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0)
        if (sector0 !is DataClassicSector) return false
        val toc = sector0.getBlock(2).data
        // Check toc entries for sectors 10,12,13,14 and 15
        return toc.byteArrayToInt(4, 2) == 0x18f0 &&
            toc.byteArrayToInt(8, 2) == 5 &&
            toc.byteArrayToInt(10, 2) == 0x18e0 &&
            toc.byteArrayToInt(12, 2) == 0x18e8
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity =
        TransitIdentity.create(NAME, formatShortSerial(getSerial(card)))

    override fun parseInfo(card: ClassicCard): StrelkaTransitInfo =
        StrelkaTransitInfo(mSerial = getSerial(card))
}
