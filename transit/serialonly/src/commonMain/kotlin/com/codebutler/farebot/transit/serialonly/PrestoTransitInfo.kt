/*
 * PrestoTransitInfo.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

class PrestoTransitInfo(
    private val mSerial: Int?,
) : SerialOnlyTransitInfo() {
    override val reason get() = Reason.LOCKED
    override val serialNumber get() = PrestoTransitFactory.formatSerial(mSerial)
    override val cardName get() = PrestoTransitFactory.NAME
}
