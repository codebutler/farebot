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
import farebot.farebot_transit_serialonly.generated.resources.Res
import farebot.farebot_transit_serialonly.generated.resources.card_name_nol
import farebot.farebot_transit_serialonly.generated.resources.card_type
import farebot.farebot_transit_serialonly.generated.resources.nol_red
import farebot.farebot_transit_serialonly.generated.resources.nol_silver
import farebot.farebot_transit_serialonly.generated.resources.unknown_format
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class NolTransitInfo(
    private val mSerial: Int?,
    private val mType: Int?
) : SerialOnlyTransitInfo() {

    override val extraInfo: List<ListItemInterface>
        get() = listOf(
            ListItem(
                Res.string.card_type,
                runBlocking {
                    when (mType) {
                        0x4d5 -> getString(Res.string.nol_silver)
                        0x4d9 -> getString(Res.string.nol_red)
                        else -> getString(Res.string.unknown_format, mType?.toString(16) ?: "?")
                    }
                }
            )
        )

    override val reason get() = Reason.LOCKED
    override val serialNumber get() = NolTransitFactory.formatSerial(mSerial)
    override val cardName get() = runBlocking { getString(Res.string.card_name_nol) }
}
