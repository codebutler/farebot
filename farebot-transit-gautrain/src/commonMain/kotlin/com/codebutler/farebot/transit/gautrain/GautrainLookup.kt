/*
 * GautrainLookup.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.gautrain

import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.en1545.En1545LookupSTR
import kotlinx.datetime.TimeZone

object GautrainLookup : En1545LookupSTR("gautrain") {
    override val timeZone: TimeZone = TimeZone.of("Africa/Johannesburg")

    override fun parseCurrency(price: Int): TransitCurrency = TransitCurrency(price, "ZAR", 10)
}
