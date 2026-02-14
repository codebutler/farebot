/*
 * ChinaCardTransitFactory.kt
 *
 * Copyright 2018-2019 Google
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

import com.codebutler.farebot.card.iso7816.ISO7816Card
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitInfo

/**
 * Interface for transit factories that work with China cards.
 *
 * China cards use the ISO 7816 smart card standard with specific Application Identifiers (AIDs)
 * for different transit systems. This interface extends [TransitFactory] to provide
 * China-specific functionality.
 *
 * Implementations should provide:
 * - [appNames]: List of AIDs that identify cards this factory can handle
 * - [check]: Verify that a specific card matches this factory
 * - [parseIdentity]: Extract the transit system name and serial number
 * - [parseInfo]: Parse the full transit information
 */
interface ChinaCardTransitFactory : TransitFactory<ISO7816Card, TransitInfo> {
    /**
     * List of Application Identifier (AID) byte arrays that this factory handles.
     * A card matches if its application name equals one of these AIDs.
     */
    val appNames: List<ByteArray>

    /**
     * Check if this factory can handle the given ISO7816Card.
     * Default implementation checks if the card has an application with a matching AID.
     */
    override fun check(card: ISO7816Card): Boolean {
        val chinaCard = ChinaCard.fromISO7816Card(card) ?: return false
        return check(chinaCard)
    }

    /**
     * Check if this factory can handle the given ChinaCard.
     * Default implementation checks if the card's app name matches one of [appNames].
     */
    fun check(card: ChinaCard): Boolean {
        val cardAppName = card.appName ?: return false
        return appNames.any { it.contentEquals(cardAppName) }
    }

    /**
     * Parse transit identity from an ISO7816Card.
     */
    override fun parseIdentity(card: ISO7816Card): TransitIdentity {
        val chinaCard =
            ChinaCard.fromISO7816Card(card)
                ?: throw IllegalArgumentException("Not a valid China card")
        return parseTransitIdentity(chinaCard)
    }

    /**
     * Parse transit identity from a ChinaCard.
     */
    fun parseTransitIdentity(card: ChinaCard): TransitIdentity

    /**
     * Parse full transit info from an ISO7816Card.
     */
    override fun parseInfo(card: ISO7816Card): TransitInfo {
        val chinaCard =
            ChinaCard.fromISO7816Card(card)
                ?: throw IllegalArgumentException("Not a valid China card")
        return parseTransitData(chinaCard)
    }

    /**
     * Parse full transit data from a ChinaCard.
     */
    fun parseTransitData(card: ChinaCard): TransitInfo
}
