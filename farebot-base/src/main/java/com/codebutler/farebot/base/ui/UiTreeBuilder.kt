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

import android.content.Context

@DslMarker
private annotation class UiTreeBuilderMarker

fun uiTree(context: Context, init: TreeScope.() -> Unit): FareBotUiTree {
    val uiBuilder = FareBotUiTree.builder(context)
    TreeScope(context, uiBuilder).init()
    return uiBuilder.build()
}

@UiTreeBuilderMarker
class TreeScope(private val context: Context, private val uiBuilder: FareBotUiTree.Builder) {
    fun item(init: ItemScope.() -> Unit) {
        ItemScope(context, uiBuilder.item()).init()
    }
}

@UiTreeBuilderMarker
class ItemScope(private val context: Context, private val item: FareBotUiTree.Item.Builder) {

    var title: Any? = null
        set(value) { item.title(if (value is Int) context.getString(value) else value.toString()) }

    var value: Any? = null
        set(value) { item.value(value) }

    fun item(init: ItemScope.() -> Unit) {
        ItemScope(context, item.item()).init()
    }
}
