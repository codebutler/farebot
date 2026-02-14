/*
 * NolTransitInfo.kt
 *
 * Copyright 2019 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.getStringBlocking
import farebot.transit.serialonly.generated.resources.Res
import farebot.transit.serialonly.generated.resources.card_name_nol
import farebot.transit.serialonly.generated.resources.card_type
import farebot.transit.serialonly.generated.resources.nol_red
import farebot.transit.serialonly.generated.resources.nol_silver
import farebot.transit.serialonly.generated.resources.unknown_format

class NolTransitInfo(
    private val mSerial: Int?,
    private val mType: Int?,
) : SerialOnlyTransitInfo() {
    override val extraInfo: List<ListItemInterface>
        get() =
            listOf(
                ListItem(
                    Res.string.card_type,
                    when (mType) {
                        0x4d5 -> getStringBlocking(Res.string.nol_silver)
                        0x4d9 -> getStringBlocking(Res.string.nol_red)
                        else -> getStringBlocking(Res.string.unknown_format, mType?.toString(16) ?: "?")
                    },
                ),
            )

    override val reason get() = Reason.LOCKED
    override val serialNumber get() = NolTransitFactory.formatSerial(mSerial)
    override val cardName get() = getStringBlocking(Res.string.card_name_nol)
}
