/*
 * OpalSubscription.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.opal

import com.codebutler.farebot.transit.Subscription
import farebot.transit.opal.generated.resources.Res
import farebot.transit.opal.generated.resources.opal_agency_tfnsw
import farebot.transit.opal.generated.resources.opal_agency_tfnsw_short
import farebot.transit.opal.generated.resources.opal_automatic_top_up
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Instant
import com.codebutler.farebot.base.util.FormattedString

/**
 * Class describing auto-topup on Opal.
 *
 * Opal has no concept of subscriptions, but when auto-topup is enabled, you no longer need to
 * manually refill the card with credit.
 *
 * Dates given are not valid.
 */
internal class OpalSubscription private constructor() : Subscription() {
    companion object {
        val instance = OpalSubscription()
    }

    // Start of Opal trial
    override val validFrom: Instant get() = LocalDate(2012, 12, 7).atStartOfDayIn(TimeZone.UTC)

    // Maximum possible date representable on the card
    override val validTo: Instant get() = LocalDate(2159, 6, 6).atStartOfDayIn(TimeZone.UTC)

    override val subscriptionName: FormattedString
        get() = FormattedString(Res.string.opal_automatic_top_up)

    override val paymentMethod: PaymentMethod get() = PaymentMethod.CREDIT_CARD

    override val agencyName: FormattedString
        get() = FormattedString(Res.string.opal_agency_tfnsw)

    override val shortAgencyName: FormattedString
        get() = FormattedString(Res.string.opal_agency_tfnsw_short)
}
