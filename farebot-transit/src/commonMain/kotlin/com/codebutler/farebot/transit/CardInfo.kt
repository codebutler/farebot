/*
 * CardInfo.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2019 Michael Farrell
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

package com.codebutler.farebot.transit

import com.codebutler.farebot.card.CardType
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * Metadata about a supported transit card.
 *
 * This class holds information about a transit card that is displayed
 * in the "Supported Cards" list, including:
 * - Card name and location
 * - Card type (MIFARE Classic, DESFire, FeliCa, etc.)
 * - Geographic region
 * - Optional card image
 * - Whether keys are required for reading
 * - Preview status (for incomplete/beta implementations)
 *
 * @property name Display name of the card
 * @property cardType The NFC card technology used
 * @property region Geographic region where this card is used
 * @property locationId Optional string resource for more specific location
 * @property keysRequired Whether authentication keys are needed to read this card
 * @property keyBundle Name of the key bundle file, if keys are required
 * @property preview Whether this is a preview/beta decoder with possibly incomplete data
 * @property extraNote Optional note about limitations or special information
 * @property imageId Optional drawable resource for the card image
 * @property imageAlphaId Optional drawable resource for the card image alpha mask
 * @property iOSSupported Whether this card type is supported on iOS
 * @property iOSExtraNote Optional note that replaces extraNote on iOS only
 */
data class CardInfo(
    val name: String,
    val cardType: CardType,
    val region: TransitRegion,
    val locationId: StringResource? = null,
    val keysRequired: Boolean = false,
    val keyBundle: String? = null,
    val preview: Boolean = false,
    val extraNote: StringResource? = null,
    val imageId: DrawableResource? = null,
    val imageAlphaId: DrawableResource? = null,
    val iOSSupported: Boolean? = null,
    val iOSExtraNote: StringResource? = null
) {
    /**
     * Whether this card has an associated image.
     */
    val hasBitmap: Boolean
        get() = imageId != null
}
