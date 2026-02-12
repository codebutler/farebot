/*
 * HoloTransitFactory.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.convertBCDtoInteger
import com.codebutler.farebot.card.desfire.DesfireApplication
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_transit_serialonly.generated.resources.*

class HoloTransitFactory : TransitFactory<DesfireCard, HoloTransitInfo> {

    override val allCards: List<CardInfo> = listOf(
        CardInfo(
            nameRes = Res.string.card_name_holo,
            cardType = CardType.MifareDesfire,
            region = TransitRegion.USA,
            locationRes = Res.string.card_location_oahu_hawaii,
            serialOnly = true,
            imageRes = Res.drawable.holo_card,
            latitude = 21.3069f,
            longitude = -157.8583f,
            brandColor = 0x00A8C4,
            sampleDumpFile = "Holo.json",
        )
    )

    companion object {
        internal const val APP_ID = 0x6013f2
        internal const val NAME = "HOLO"

        internal fun parseSerial(app: DesfireApplication?): Int? {
            val data = (app?.getFile(0) as? StandardDesfireFile)?.data ?: return null
            return data.convertBCDtoInteger(0xe, 2)
        }

        internal fun formatSerial(ser: Int?): String? =
            if (ser != null) "31059300 1 ***** *$ser" else null
    }

    override fun check(card: DesfireCard): Boolean =
        card.getApplication(APP_ID) != null

    override fun parseIdentity(card: DesfireCard): TransitIdentity =
        TransitIdentity.create(NAME, formatSerial(parseSerial(card.getApplication(APP_ID))))

    override fun parseInfo(card: DesfireCard): HoloTransitInfo {
        val app = card.getApplication(APP_ID) ?: return HoloTransitInfo(null, 0, "")
        val file0 = (app.getFile(0) as? StandardDesfireFile)?.data
        val file1 = (app.getFile(1) as? StandardDesfireFile)?.data
        val serial = parseSerial(app)

        val mfgId = if (file0 != null) {
            "1-001-${file0.convertBCDtoInteger(8, 3)}${file0.byteArrayToInt(0xb, 3)}-XA"
        } else {
            ""
        }
        val lastTransactionTimestamp = file1?.byteArrayToInt(8, 4) ?: 0

        return HoloTransitInfo(
            mSerial = serial,
            mLastTransactionTimestamp = lastTransactionTimestamp,
            mManufacturingId = mfgId
        )
    }
}
