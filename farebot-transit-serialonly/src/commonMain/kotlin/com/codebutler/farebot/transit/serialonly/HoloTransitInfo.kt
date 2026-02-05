/*
 * HoloTransitInfo.kt
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
import farebot.farebot_transit_serialonly.generated.resources.last_transaction
import farebot.farebot_transit_serialonly.generated.resources.manufacture_id
import farebot.farebot_transit_serialonly.generated.resources.never
import kotlinx.coroutines.runBlocking
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

class HoloTransitInfo(
    private val mSerial: Int?,
    private val mLastTransactionTimestamp: Int,
    private val mManufacturingId: String
) : SerialOnlyTransitInfo() {

    override val extraInfo: List<ListItemInterface>
        get() = listOf(
            ListItem(
                Res.string.last_transaction,
                when (mLastTransactionTimestamp) {
                    0 -> runBlocking { getString(Res.string.never) }
                    else -> {
                        val instant = Instant.fromEpochSeconds(mLastTransactionTimestamp.toLong())
                        val local = instant.toLocalDateTime(TimeZone.of("Pacific/Honolulu"))
                        "${local.date} ${local.hour}:${local.minute.toString().padStart(2, '0')}"
                    }
                }
            ),
            ListItem(Res.string.manufacture_id, mManufacturingId)
        )

    override val reason get() = Reason.NOT_STORED
    override val cardName get() = HoloTransitFactory.NAME
    override val serialNumber get() = HoloTransitFactory.formatSerial(mSerial)
}
