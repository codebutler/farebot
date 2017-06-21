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

package com.codebutler.farebot.app.core.kotlin

import android.content.Context
import com.codebutler.farebot.base.ui.FareBotUiTree

@DslMarker
private annotation class UiTreeBuilderMarker

fun uiTree(context: Context, init: TreeScope.() -> Unit): FareBotUiTree {
    val uiBuilder = FareBotUiTree.builder(context)
    TreeScope(context, uiBuilder).init()
    return uiBuilder.build()
}

@UiTreeBuilderMarker
class TreeScope(val context: Context, val uiBuilder: FareBotUiTree.Builder) {
    fun item(init: ItemScope.() -> Unit) {
        ItemScope(context, uiBuilder.item()).init()
    }
}

@UiTreeBuilderMarker
class ItemScope(val context: Context, val item: FareBotUiTree.Item.Builder) {
    fun item(init: ItemScope.() -> Unit) {
        ItemScope(context, item.item()).init()
    }
    fun title(init: ContentScope.() -> Any) {
        val title = ContentScope().init()
        item.title(if (title is Int) context.getString(title) else title.toString())
    }
    fun value(init: ContentScope.() -> Any) {
        item.value(ContentScope().init())
    }
}

@UiTreeBuilderMarker
class ContentScope()
