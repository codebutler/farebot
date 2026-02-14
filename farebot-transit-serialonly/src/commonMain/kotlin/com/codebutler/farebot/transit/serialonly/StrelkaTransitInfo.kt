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

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.getStringBlocking
import farebot.farebot_transit_serialonly.generated.resources.Res
import farebot.farebot_transit_serialonly.generated.resources.card_name_strelka
import farebot.farebot_transit_serialonly.generated.resources.strelka_long_serial

class StrelkaTransitInfo(
    private val mSerial: String,
) : SerialOnlyTransitInfo() {
    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree {
        val b = FareBotUiTree.builder(stringResource)
        b.item().title(Res.string.strelka_long_serial).value(mSerial)
        return b.build()
    }

    override val reason get() = Reason.MORE_RESEARCH_NEEDED
    override val serialNumber get() = StrelkaTransitFactory.formatShortSerial(mSerial)
    override val cardName get() = getStringBlocking(Res.string.card_name_strelka)
}
