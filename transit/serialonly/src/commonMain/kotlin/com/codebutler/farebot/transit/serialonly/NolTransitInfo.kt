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
import farebot.transit.serialonly.generated.resources.Res
import farebot.transit.serialonly.generated.resources.card_name_nol
import farebot.transit.serialonly.generated.resources.card_type
import farebot.transit.serialonly.generated.resources.nol_red
import farebot.transit.serialonly.generated.resources.nol_silver
import farebot.transit.serialonly.generated.resources.unknown_format
import com.codebutler.farebot.base.util.FormattedString

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
                        0x4d5 -> FormattedString(Res.string.nol_silver)
                        0x4d9 -> FormattedString(Res.string.nol_red)
                        else -> FormattedString(Res.string.unknown_format, mType?.toString(16) ?: "?")
                    },
                ),
            )

    override val reason get() = Reason.LOCKED
    override val serialNumber get() = NolTransitFactory.formatSerial(mSerial)
    override val cardName get() = FormattedString(Res.string.card_name_nol)
}
