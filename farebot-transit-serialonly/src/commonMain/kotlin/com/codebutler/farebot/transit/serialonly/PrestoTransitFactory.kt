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
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

class PrestoTransitFactory : TransitFactory<DesfireCard, PrestoTransitInfo> {

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
