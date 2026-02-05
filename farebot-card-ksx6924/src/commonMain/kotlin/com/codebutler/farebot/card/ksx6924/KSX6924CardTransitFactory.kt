/*
 * KSX6924CardTransitFactory.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.card.ksx6924

import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo

/**
 * Interface for transit card factories that handle KSX6924-based cards.
 *
 * This is similar to the general TransitFactory but specifically for
 * KSX6924 applications (T-Money, Snapper, Cashbee, etc.).
 *
 * Implementations should be registered with [KSX6924Registry].
 */
interface KSX6924CardTransitFactory {

    /**
     * Checks if this factory can handle the given KSX6924 application.
     *
     * @param app The KSX6924 application to check.
     * @return true if this factory can handle the application.
     */
    fun check(app: KSX6924Application): Boolean

    /**
     * Parses the transit identity from the KSX6924 application.
     *
     * @param app The KSX6924 application to parse.
     * @return The transit identity, or null if parsing failed.
     */
    fun parseTransitIdentity(app: KSX6924Application): TransitIdentity?

    /**
     * Parses the transit data from the KSX6924 application.
     *
     * @param app The KSX6924 application to parse.
     * @return The transit info, or null if parsing failed.
     */
    fun parseTransitData(app: KSX6924Application): TransitInfo?
}
