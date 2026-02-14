/*
 * StrelkaTransitInfo.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemCategory
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.getStringBlocking
import farebot.farebot_transit_serialonly.generated.resources.Res
import farebot.farebot_transit_serialonly.generated.resources.card_name_strelka
import farebot.farebot_transit_serialonly.generated.resources.strelka_long_serial

class StrelkaTransitInfo(
    private val mSerial: String,
) : SerialOnlyTransitInfo() {
    public override val extraInfo: List<ListItemInterface>
        get() = listOf(ListItem(Res.string.strelka_long_serial, mSerial, ListItemCategory.ADVANCED))

    override val reason get() = Reason.MORE_RESEARCH_NEEDED
    override val serialNumber get() = StrelkaTransitFactory.formatShortSerial(mSerial)
    override val cardName get() = getStringBlocking(Res.string.card_name_strelka)
}
