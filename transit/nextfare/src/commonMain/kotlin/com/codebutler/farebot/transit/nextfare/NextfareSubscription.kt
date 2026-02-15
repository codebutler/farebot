/*
 * NextfareSubscription.kt
 *
 * Copyright 2016-2017 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.nextfare

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.nextfare.record.NextfareBalanceRecord
import com.codebutler.farebot.transit.nextfare.record.NextfareTravelPassRecord
import farebot.transit.nextfare.generated.resources.Res
import farebot.transit.nextfare.generated.resources.nextfare_agency_name
import farebot.transit.nextfare.generated.resources.nextfare_travel_pass
import farebot.transit.nextfare.generated.resources.nextfare_travel_pass_unused
import kotlin.time.Instant

/**
 * Represents a Nextfare travel pass subscription.
 */
class NextfareSubscription private constructor(
    private val validToValue: Instant?,
    private val isActive: Boolean,
) : Subscription() {
    /**
     * Create from a travel pass record (active subscription).
     */
    constructor(record: NextfareTravelPassRecord) : this(
        validToValue = record.timestamp,
        isActive = true,
    )

    /**
     * Create from a balance record (subscription available but not yet started).
     */
    @Suppress("UNUSED_PARAMETER")
    constructor(record: NextfareBalanceRecord) : this(
        validToValue = null,
        isActive = false,
    )

    override val id: Int = 0

    override val validFrom: Instant = Instant.DISTANT_PAST

    override val validTo: Instant? get() = validToValue ?: Instant.DISTANT_FUTURE

    override val agencyName: FormattedString
        get() = FormattedString(Res.string.nextfare_agency_name)

    override val shortAgencyName: FormattedString get() = agencyName

    override val machineId: Int = 0

    override val subscriptionName: FormattedString
        get() =
            if (isActive) {
                FormattedString(Res.string.nextfare_travel_pass)
            } else {
                FormattedString(Res.string.nextfare_travel_pass_unused)
            }
}
