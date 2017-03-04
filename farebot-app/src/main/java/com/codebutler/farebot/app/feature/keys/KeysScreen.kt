/*
 * KeysScreen.kt
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

package com.codebutler.farebot.app.feature.keys

import android.content.Context
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.inject.ScreenScope
import com.codebutler.farebot.app.core.ui.FareBotScreen
import com.codebutler.farebot.app.feature.main.MainActivity
import dagger.Component

class KeysScreen : FareBotScreen<KeysScreen.KeysComponent, KeysScreenView>() {

    override fun getTitle(context: Context): String = context.getString(R.string.keys)

    override fun onCreateView(context: Context): KeysScreenView = KeysScreenView(context)

    override fun createComponent(parentComponent: MainActivity.MainActivityComponent): KeysScreen.KeysComponent
            = DaggerKeysScreen_KeysComponent.builder()
            .mainActivityComponent(parentComponent)
            .build()

    override fun inject(component: KeysScreen.KeysComponent) {
        component.inject(this)
    }

    @ScreenScope
    @Component(dependencies = arrayOf(MainActivity.MainActivityComponent::class))
    interface KeysComponent {

        fun inject(screen: KeysScreen)
    }
}
