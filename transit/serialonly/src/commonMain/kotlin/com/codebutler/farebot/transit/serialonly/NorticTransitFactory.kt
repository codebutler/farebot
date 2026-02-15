/*
 * NorticTransitFactory.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018-2022 Google Inc.
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.base.util.getBitsFromBuffer
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity

class NorticTransitFactory : TransitFactory<DesfireCard, NorticTransitInfo> {
    override val allCards: List<CardInfo> = emptyList()

    companion object {
        internal const val APP_ID = 0x8057

        internal fun parse(card: DesfireCard): NorticTransitInfo? {
            val ciHeader =
                (card.getApplication(APP_ID)?.getFile(0xc) as? StandardDesfireFile)
                    ?.data ?: return null

            return NorticTransitInfo(
                mCountry = ciHeader.getBitsFromBuffer(0, 10),
                mFormat = ciHeader.getBitsFromBuffer(10, 20),
                mCardIdSelector = ciHeader.getBitsFromBuffer(30, 2),
                mSerial = ciHeader.byteArrayToLong(4, 4),
                mValidityEndDate = ciHeader.getBitsFromBuffer(64, 14),
                mOwnerCompany = ciHeader.getBitsFromBuffer(78, 20),
                mRetailerCompany = ciHeader.getBitsFromBuffer(98, 20),
                mCardKeyVersion = ciHeader.getBitsFromBuffer(118, 4),
            )
        }
    }

    override fun check(card: DesfireCard): Boolean = card.getApplication(APP_ID) != null

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        val ciHeader =
            (card.getApplication(APP_ID)?.getFile(0xc) as? StandardDesfireFile)?.data
                ?: return TransitIdentity.create(FormattedString("Nortic"), null)
        val serial = ciHeader.byteArrayToLong(4, 4)
        val ownerCompany = ciHeader.getBitsFromBuffer(78, 20)
        return TransitIdentity.create(
            NorticTransitInfo.getName(ownerCompany),
            NorticTransitInfo.formatSerial(ownerCompany, serial),
        )
    }

    override fun parseInfo(card: DesfireCard): NorticTransitInfo =
        parse(card) ?: NorticTransitInfo(0, 0, 0, 0, 0, 0, 0, 0)
}
