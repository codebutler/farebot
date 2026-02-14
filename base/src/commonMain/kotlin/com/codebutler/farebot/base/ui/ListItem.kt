/*
 * ListItem.kt
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

import com.codebutler.farebot.base.util.getStringBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

@Serializable
@SerialName("normal")
data class ListItem(
    override val text1: String?,
    override val text2: String?,
) : ListItemInterface() {
    constructor(name: String) : this(name, null)

    constructor(nameRes: StringResource, value: String?) : this(
        text1 = getStringBlocking(nameRes),
        text2 = value,
    )

    constructor(nameRes: StringResource) : this(
        text1 = getStringBlocking(nameRes),
        text2 = null,
    )

    /**
     * Constructor for format strings with arguments.
     * The nameRes should be a format string like "%s spend".
     */
    constructor(nameRes: StringResource, value: String?, vararg formatArgs: Any) : this(
        text1 = getStringBlocking(nameRes, *formatArgs),
        text2 = value,
    )

    /**
     * Constructor for two StringResources.
     */
    constructor(nameRes: StringResource, valueRes: StringResource) : this(
        text1 = getStringBlocking(nameRes),
        text2 = getStringBlocking(valueRes),
    )
}
