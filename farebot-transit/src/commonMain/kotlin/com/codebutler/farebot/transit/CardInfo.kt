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

data class CardInfo(
    val nameRes: StringResource,
    val cardType: CardType,
    val region: TransitRegion,
    val locationRes: StringResource,
    val keysRequired: Boolean = false,
    val keyBundle: String? = null,
    val preview: Boolean = false,
    val serialOnly: Boolean = false,
    val extraNoteRes: StringResource? = null,
    val imageRes: DrawableResource? = null,
    val latitude: Float? = null,
    val longitude: Float? = null,
)
