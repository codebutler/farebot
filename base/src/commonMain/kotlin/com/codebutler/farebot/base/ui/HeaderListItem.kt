/*
 * HeaderListItem.kt
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
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

package com.codebutler.farebot.base.ui

import com.codebutler.farebot.base.util.FormattedString
import org.jetbrains.compose.resources.StringResource

data class HeaderListItem(
    override val text1: FormattedString?,
    val headingLevel: Int = 2,
) : ListItemInterface() {
    constructor(title: String) : this(FormattedString(title), 2)

    constructor(titleRes: StringResource) : this(
        text1 = FormattedString(titleRes),
        headingLevel = 2,
    )

    override val text2: FormattedString?
        get() = null
}
