/*
 * UnknownTransitInfo.kt
 *
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

package com.codebutler.farebot.transit

import com.codebutler.farebot.base.util.FormattedString

/**
 * Fallback TransitInfo for cards that no transit factory recognized.
 * Shows basic card metadata (type, tag ID) instead of a raw error message.
 */
class UnknownTransitInfo(
    private val cardTypeName: String,
    private val tagIdHex: String,
    private val isPartialRead: Boolean = false,
) : TransitInfo() {
    override val serialNumber: String = tagIdHex

    override val cardName: FormattedString =
        if (isPartialRead) {
            FormattedString("$cardTypeName (Partial Read)")
        } else {
            FormattedString("$cardTypeName (Unrecognized)")
        }
}
