/*
 * TrimetHopTransitInfo.kt
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
import farebot.farebot_transit_serialonly.generated.resources.issue_date
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class TrimetHopTransitInfo(
    private val mSerial: Int?,
    private val mIssueDate: Int?
) : SerialOnlyTransitInfo() {

    override val extraInfo: List<ListItemInterface>?
        get() = mIssueDate?.let {
            val instant = Instant.fromEpochSeconds(it.toLong())
            val local = instant.toLocalDateTime(TimeZone.of("America/Los_Angeles"))
            listOf(ListItem(Res.string.issue_date, "${local.date} ${local.hour}:${local.minute.toString().padStart(2, '0')}"))
        }

    override val reason get() = Reason.NOT_STORED
    override val cardName get() = TrimetHopTransitFactory.NAME
    override val serialNumber get() = TrimetHopTransitFactory.formatSerial(mSerial)
}
