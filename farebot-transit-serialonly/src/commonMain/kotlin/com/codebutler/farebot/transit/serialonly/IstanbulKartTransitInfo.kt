/*
 * IstanbulKartTransitInfo.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import farebot.farebot_transit_serialonly.generated.resources.Res
import farebot.farebot_transit_serialonly.generated.resources.istanbulkart_2nd_card_number

class IstanbulKartTransitInfo(
    private val mSerial: String,
    private val mSerial2: String
) : SerialOnlyTransitInfo() {

    override val extraInfo: List<ListItemInterface>
        get() = listOf(ListItem(Res.string.istanbulkart_2nd_card_number, mSerial2))

    override val reason get() = Reason.LOCKED
    override val cardName get() = IstanbulKartTransitFactory.NAME
    override val serialNumber get() = IstanbulKartTransitFactory.formatSerial(mSerial)
}
