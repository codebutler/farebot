/*
 * IntercodeLookup.kt
 *
 * Copyright 2018 Google
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

package com.codebutler.farebot.transit.calypso.intercode

import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.en1545.En1545Lookup
import com.codebutler.farebot.transit.en1545.En1545Parsed
import kotlinx.datetime.TimeZone

interface IntercodeLookup : En1545Lookup {
    /**
     * Returns the card name for this network.
     * We pass a function rather than the env itself because most lookups do not need any
     * additional info and so in most cases we can avoid completely parsing the ticketing
     * environment just to get the card name.
     */
    fun cardName(env: () -> En1545Parsed): FormattedString?

    /** All known card names for this lookup. */
    val allCardNames: List<FormattedString>

    override val timeZone: TimeZone
        get() = TimeZone.of("Europe/Paris")

    override fun parseCurrency(price: Int): TransitCurrency = TransitCurrency.EUR(price)
}

interface IntercodeLookupSingle : IntercodeLookup {
    val cardName: FormattedString?

    override fun cardName(env: () -> En1545Parsed): FormattedString? = cardName

    override val allCardNames: List<FormattedString>
        get() = listOfNotNull(cardName)
}
