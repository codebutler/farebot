/*
 * CardAdvancedScreenView.kt
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

package com.codebutler.farebot.app.feature.card.advanced

import android.content.Context
import android.view.ViewGroup
import android.widget.TabHost
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.kotlin.inflate
import com.codebutler.farebot.app.core.kotlin.bindView
import com.codebutler.farebot.base.ui.FareBotUiTree
import com.wealthfront.magellan.BaseScreenView

class CardAdvancedScreenView(context: Context) : BaseScreenView<CardAdvancedScreen>(context) {

    private val tabHost: TabHost by bindView(android.R.id.tabhost)
    private val tabContent: ViewGroup by bindView(android.R.id.tabcontent)

    private var tabCount = 0

    init {
        inflate(context, R.layout.screen_card_advanced, this)
        tabHost.setup()
    }

    fun addTab(title: String, fareBotUiTree: FareBotUiTree) {
        val contentView = tabContent.inflate(R.layout.tab_card_advanced, false) as CardAdvancedTabView
        contentView.setAdvancedUi(fareBotUiTree)
        tabHost.addTab(tabHost.newTabSpec("tab_$tabCount")
                .setIndicator(title)
                .setContent { contentView })
        tabCount++
    }
}
