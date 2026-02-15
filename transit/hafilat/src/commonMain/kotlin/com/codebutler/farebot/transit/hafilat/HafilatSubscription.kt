/*
 * HafilatSubscription.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.hafilat

import com.codebutler.farebot.transit.calypso.IntercodeFields
import com.codebutler.farebot.transit.en1545.En1545Parsed
import com.codebutler.farebot.transit.en1545.En1545Parser
import com.codebutler.farebot.transit.en1545.En1545Subscription

class HafilatSubscription(
    override val parsed: En1545Parsed,
) : En1545Subscription() {
    override val lookup: HafilatLookup
        get() = HafilatLookup

    val isPurse: Boolean
        get() = lookup.isPurseTariff(contractProvider, contractTariff)

    companion object {
        fun parse(
            data: ByteArray,
        ): HafilatSubscription =
            HafilatSubscription(En1545Parser.parse(data, IntercodeFields.SUB_FIELDS_TYPE_46))
    }
}
