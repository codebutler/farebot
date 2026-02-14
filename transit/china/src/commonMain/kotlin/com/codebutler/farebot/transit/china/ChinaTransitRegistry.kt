/*
 * ChinaTransitRegistry.kt
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

package com.codebutler.farebot.transit.china

import com.codebutler.farebot.card.china.ChinaCardTransitFactory
import com.codebutler.farebot.card.china.ChinaRegistry

/**
 * Registry for China transit card factories.
 *
 * This object registers all known China transit system factories with the ChinaRegistry.
 * Call [registerAll] at app startup to enable detection and parsing of China transit cards.
 *
 * Supported systems:
 * - Beijing Municipal Card (BMAC / 北京市政交通一卡通)
 * - City Union (城市一卡通) - Shanghai and other cities
 * - Shenzhen Tong (深圳通) - New format
 * - T-Union (交通联合) - Nationwide interoperability
 * - Wuhan Tong (武汉通)
 */
object ChinaTransitRegistry {
    /**
     * All available China transit card factories.
     */
    val allFactories: List<ChinaCardTransitFactory> =
        listOf(
            // Order matters - more specific factories should come first
            NewShenzhenTransitInfo.FACTORY,
            WuhanTongTransitInfo.FACTORY,
            TUnionTransitInfo.FACTORY,
            CityUnionTransitInfo.FACTORY,
            BeijingTransitInfo.FACTORY, // Most generic, check last
        )

    /**
     * Register all China transit factories with the ChinaRegistry.
     * Call this at application startup.
     */
    fun registerAll() {
        allFactories.forEach { factory ->
            ChinaRegistry.registerFactory(factory)
        }
    }

    /**
     * Unregister all China transit factories.
     * Primarily for testing purposes.
     */
    fun unregisterAll() {
        allFactories.forEach { factory ->
            ChinaRegistry.unregisterFactory(factory)
        }
    }
}
