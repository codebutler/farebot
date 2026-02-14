/*
 * AtHopTransitInfo.kt
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

class AtHopTransitInfo(
    private val mSerial: Int?,
) : SerialOnlyTransitInfo() {
    override val reason get() = Reason.LOCKED
    override val serialNumber get() = AtHopTransitFactory.formatSerial(mSerial)
    override val cardName get() = AtHopTransitFactory.NAME
}
