/*
 * TPFCardTransitInfo.kt
 *
 * Copyright 2019 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import farebot.farebot_transit_serialonly.generated.resources.Res
import farebot.farebot_transit_serialonly.generated.resources.card_name_tpf
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class TPFCardTransitInfo(private val mSerial: String) : SerialOnlyTransitInfo() {
    override val reason get() = Reason.LOCKED
    override val serialNumber get() = mSerial
    override val cardName get() = NAME

    companion object {
        internal val NAME by lazy { runBlocking { getString(Res.string.card_name_tpf) } }
    }
}
