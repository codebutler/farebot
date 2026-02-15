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

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.FormattedString
import farebot.transit.serialonly.generated.resources.Res
import farebot.transit.serialonly.generated.resources.last_transaction
import farebot.transit.serialonly.generated.resources.manufacture_id
import farebot.transit.serialonly.generated.resources.never
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class HoloTransitInfo(
    private val mSerial: Int?,
    private val mLastTransactionTimestamp: Int,
    private val mManufacturingId: String,
) : SerialOnlyTransitInfo() {
    override val extraInfo: List<ListItemInterface>
        get() =
            listOf(
                ListItem(
                    Res.string.last_transaction,
                    when (mLastTransactionTimestamp) {
                        0 -> FormattedString(Res.string.never)
                        else -> {
                            val instant = Instant.fromEpochSeconds(mLastTransactionTimestamp.toLong())
                            val local = instant.toLocalDateTime(TimeZone.of("Pacific/Honolulu"))
                            FormattedString("${local.date} ${local.hour}:${local.minute.toString().padStart(2, '0')}")
                        }
                    },
                ),
            )

    override suspend fun getAdvancedUi(): FareBotUiTree {
        val b = FareBotUiTree.builder()
        b.item().title(Res.string.manufacture_id).value(mManufacturingId)
        return b.build()
    }

    override val reason get() = Reason.NOT_STORED
    override val cardName get() = HoloTransitFactory.NAME
    override val serialNumber get() = HoloTransitFactory.formatSerial(mSerial)
}
