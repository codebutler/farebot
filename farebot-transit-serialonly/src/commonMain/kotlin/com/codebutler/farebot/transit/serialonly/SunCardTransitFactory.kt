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
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

class SunCardTransitFactory : TransitFactory<ClassicCard, SunCardTransitInfo> {

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
