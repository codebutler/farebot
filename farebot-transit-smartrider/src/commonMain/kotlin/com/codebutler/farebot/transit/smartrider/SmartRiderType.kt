/*
 * SmartRiderType.kt
 *
 * Copyright 2016-2022 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.smartrider

import farebot.farebot_transit_smartrider.generated.resources.Res
import farebot.farebot_transit_smartrider.generated.resources.card_name_myway
import farebot.farebot_transit_smartrider.generated.resources.card_name_smartrider
import farebot.farebot_transit_smartrider.generated.resources.unknown
import org.jetbrains.compose.resources.StringResource

enum class SmartRiderType(
    val friendlyName: StringResource,
) {
    UNKNOWN(Res.string.unknown),
    SMARTRIDER(Res.string.card_name_smartrider),
    MYWAY(Res.string.card_name_myway),
}
