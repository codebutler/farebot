/*
 * ChinaRegistry.kt
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

package com.codebutler.farebot.card.china

/**
 * Registry of all China card transit factories.
 *
 * This registry maintains a list of all known China transit system factories.
 * Transit implementations should register their factories here to be recognized
 * when reading China-based transit cards.
 *
 * Supported transit systems:
 * - Beijing Municipal Card (BMAC)
 * - City Union (various Chinese cities)
 * - New Shenzhen Tong
 * - T-Union (Transportation Union)
 * - Wuhan Tong
 */
object ChinaRegistry {

    /**
     * Mutable list for factory registration.
     */
    private val factories = mutableListOf<ChinaCardTransitFactory>()

    /**
     * List of all registered China card transit factories.
     * Factories are checked in registration order.
     */
    val allFactories: List<ChinaCardTransitFactory>
        get() = factories.toList()

    /**
     * Register a China card transit factory.
     * Factories registered first have priority when matching cards.
     */
    fun registerFactory(factory: ChinaCardTransitFactory) {
        factories.add(factory)
    }

    /**
     * Unregister a China card transit factory.
     */
    fun unregisterFactory(factory: ChinaCardTransitFactory) {
        factories.remove(factory)
    }

    /**
     * Clear all registered factories.
     * Primarily for testing purposes.
     */
    fun clear() {
        factories.clear()
    }

    /**
     * Get all known Application Identifiers (AIDs) from all registered factories.
     */
    val allAppNames: List<ByteArray>
        get() = allFactories.flatMap { it.appNames }

    /**
     * Find a factory that can handle the given ChinaCard.
     * Returns the first matching factory or null if none match.
     */
    fun findFactory(card: ChinaCard): ChinaCardTransitFactory? =
        allFactories.find { it.check(card) }
}
