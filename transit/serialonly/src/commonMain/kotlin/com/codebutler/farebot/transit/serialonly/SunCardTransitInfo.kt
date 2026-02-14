/*
 * SunCardTransitInfo.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import farebot.transit.serialonly.generated.resources.Res
import farebot.transit.serialonly.generated.resources.barcode_serial
import farebot.transit.serialonly.generated.resources.full_serial_number

class SunCardTransitInfo(
    private val mSerial: Int,
) : SerialOnlyTransitInfo() {
    override val extraInfo: List<ListItemInterface>
        get() =
            listOf(
                ListItem(Res.string.full_serial_number, SunCardTransitFactory.formatLongSerial(mSerial)),
                ListItem(Res.string.barcode_serial, SunCardTransitFactory.formatBarcodeSerial(mSerial)),
            )

    override val reason get() = Reason.NOT_STORED
    override val serialNumber get() = SunCardTransitFactory.formatSerial(mSerial)
    override val cardName get() = SunCardTransitFactory.NAME
}
