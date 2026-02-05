/*
 * TrimetHopTransitFactory.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.card.desfire.DesfireApplication
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

class TrimetHopTransitFactory : TransitFactory<DesfireCard, TrimetHopTransitInfo> {

    companion object {
        internal const val APP_ID = 0xe010f2
        internal const val NAME = "Hop Fastpass"

        internal fun parseSerial(app: DesfireApplication?): Int? =
            (app?.getFile(0) as? StandardDesfireFile)?.data?.byteArrayToInt(0xc, 4)

        internal fun formatSerial(ser: Int?): String? =
            if (ser != null) "01-001-${NumberUtils.zeroPad(ser, 8)}-RA" else null
    }

    override fun check(card: DesfireCard): Boolean =
        card.getApplication(APP_ID) != null

    override fun parseIdentity(card: DesfireCard): TransitIdentity =
        TransitIdentity.create(NAME, formatSerial(parseSerial(card.getApplication(APP_ID))))

    override fun parseInfo(card: DesfireCard): TrimetHopTransitInfo {
        val app = card.getApplication(APP_ID)
        val file1 = (app?.getFile(1) as? StandardDesfireFile)?.data
        val serial = parseSerial(app)
        val issueDate = file1?.byteArrayToInt(8, 4)

        return TrimetHopTransitInfo(mSerial = serial, mIssueDate = issueDate)
    }
}
