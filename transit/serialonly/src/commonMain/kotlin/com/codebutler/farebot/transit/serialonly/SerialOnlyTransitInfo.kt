/*
 * SerialOnlyTransitInfo.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.serialonly.generated.resources.Res
import farebot.transit.serialonly.generated.resources.serial_only_card_description_locked
import farebot.transit.serialonly.generated.resources.serial_only_card_description_more_research
import farebot.transit.serialonly.generated.resources.serial_only_card_description_not_stored

abstract class SerialOnlyTransitInfo : TransitInfo() {
    protected open val extraInfo: List<ListItemInterface>?
        get() = null

    protected abstract val reason: Reason

    final override val info: List<ListItemInterface>?
        get() = extraInfo

    override val emptyStateMessage: FormattedString
        get() =
            when (reason) {
                Reason.NOT_STORED -> FormattedString(Res.string.serial_only_card_description_not_stored)
                Reason.LOCKED -> FormattedString(Res.string.serial_only_card_description_locked)
                Reason.MORE_RESEARCH_NEEDED -> FormattedString(Res.string.serial_only_card_description_more_research)
                else -> FormattedString(Res.string.serial_only_card_description_more_research)
            }

    override val trips: List<Trip>? get() = null

    enum class Reason {
        UNSPECIFIED,
        NOT_STORED,
        LOCKED,
        MORE_RESEARCH_NEEDED,
    }
}
