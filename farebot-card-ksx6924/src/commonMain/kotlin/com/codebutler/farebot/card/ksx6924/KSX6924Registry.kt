/*
 * KSX6924Registry.kt
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
 * Registry of KSX6924 transit card factories.
 *
 * This registry maintains a list of all known KSX6924-based transit systems
 * and provides methods to parse cards.
 *
 * Transit factories should be added to [allFactories] in priority order.
 * When parsing a card, factories are tried in order until one succeeds.
 */
object KSX6924Registry {
    /**
     * All registered KSX6924 transit factories.
     *
     * Order matters - factories are tried in sequence until one matches.
     * More specific factories should come before more general ones.
     *
     * Currently empty - will be populated when TMoney, Snapper, etc. are ported.
     */
    val allFactories: MutableList<KSX6924CardTransitFactory> = mutableListOf()

    /**
     * Registers a new KSX6924 transit factory.
     *
     * @param factory The factory to register.
     */
    fun register(factory: KSX6924CardTransitFactory) {
        allFactories.add(factory)
    }

    /**
     * Parses the transit identity from a KSX6924 application.
     *
     * Tries all registered factories in order until one succeeds.
     *
     * @param app The KSX6924 application to parse.
     * @return The transit identity, or null if no factory could parse the card.
     */
    fun parseTransitIdentity(app: KSX6924Application): TransitIdentity? {
        for (factory in allFactories) {
            if (factory.check(app)) {
                return factory.parseTransitIdentity(app)
            }
        }
        return null
    }

    /**
     * Parses the transit data from a KSX6924 application.
     *
     * Tries all registered factories in order until one succeeds.
     *
     * @param app The KSX6924 application to parse.
     * @return The transit info, or null if no factory could parse the card.
     */
    fun parseTransitData(app: KSX6924Application): TransitInfo? {
        for (factory in allFactories) {
            if (factory.check(app)) {
                val data = factory.parseTransitData(app)
                if (data != null) {
                    return data
                }
            }
        }
        return null
    }
}
