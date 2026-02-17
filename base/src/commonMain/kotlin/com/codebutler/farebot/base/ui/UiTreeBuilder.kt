/*
 * UiTreeBuilder.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
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

@DslMarker
private annotation class UiTreeBuilderMarker

fun uiTree(init: TreeScope.() -> Unit): FareBotUiTree {
    val scope = TreeScope()
    scope.init()
    return FareBotUiTree(scope.items.toList())
}

@UiTreeBuilderMarker
class TreeScope {
    internal val items = mutableListOf<FareBotUiTree.Item>()

    fun item(init: ItemScope.() -> Unit) {
        val scope = ItemScope()
        scope.init()
        items.add(scope.build())
    }
}

@UiTreeBuilderMarker
class ItemScope {
    private var _title: FormattedString = FormattedString("")
    var title: Any?
        get() = _title
        set(value) {
            _title =
                when (value) {
                    is FormattedString -> value
                    is StringResource -> FormattedString(value)
                    else -> FormattedString(value.toString())
                }
        }

    var value: Any? = null

    private val children = mutableListOf<FareBotUiTree.Item>()

    fun item(init: ItemScope.() -> Unit) {
        val scope = ItemScope()
        scope.init()
        children.add(scope.build())
    }

    fun addChildren(items: List<FareBotUiTree.Item>) {
        children.addAll(items)
    }

    internal fun build(): FareBotUiTree.Item =
        FareBotUiTree.Item(
            title = _title,
            value = value,
            children = children.toList(),
        )
}
