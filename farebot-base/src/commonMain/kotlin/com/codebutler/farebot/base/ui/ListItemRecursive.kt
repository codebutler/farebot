/*
 * ListItemRecursive.kt
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

package com.codebutler.farebot.base.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("recursive")
data class ListItemRecursive(
    override val text1: String?,
    override val text2: String?,
    val subTree: List<ListItemInterface>?,
    override val category: ListItemCategory = ListItemCategory.NORMAL,
) : ListItemInterface() {
    companion object {
        fun collapsedValue(
            name: String,
            value: String?,
        ): ListItemInterface = collapsedValue(name, null, value)

        fun collapsedValue(
            title: String,
            subtitle: String?,
            value: String?,
        ): ListItemInterface =
            ListItemRecursive(
                title,
                subtitle,
                if (value != null) listOf(ListItem(null, value)) else null,
            )
    }
}
