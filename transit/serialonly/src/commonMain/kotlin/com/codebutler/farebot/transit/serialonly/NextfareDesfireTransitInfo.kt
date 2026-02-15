/*
 * NextfareDesfireTransitInfo.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.util.FormattedString
import farebot.transit.serialonly.generated.resources.Res
import farebot.transit.serialonly.generated.resources.card_name_nextfare_desfire

class NextfareDesfireTransitInfo(
    private val mSerial: Long,
) : SerialOnlyTransitInfo() {
    override val reason get() = Reason.LOCKED
    override val serialNumber get() = NextfareDesfireTransitFactory.formatSerial(mSerial)
    override val cardName get() = NAME

    companion object {
        internal val NAME by lazy { FormattedString(Res.string.card_name_nextfare_desfire) }
    }
}
