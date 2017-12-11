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
import android.view.Menu
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.activity.ActivityOperations
import com.codebutler.farebot.app.core.inject.ScreenScope
import com.codebutler.farebot.app.core.ui.ActionBarOptions
import com.codebutler.farebot.app.core.ui.FareBotScreen
import com.codebutler.farebot.app.core.util.ErrorUtils
import com.codebutler.farebot.app.feature.keys.add.AddKeyScreen
import com.codebutler.farebot.app.feature.main.MainActivity
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.db.model.SavedKey
import com.uber.autodispose.kotlin.autoDisposable
import dagger.Component
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class KeysScreen : FareBotScreen<KeysScreen.KeysComponent, KeysScreenView>(), KeysScreenView.Listener {

    @Inject lateinit var activityOperations: ActivityOperations
    @Inject lateinit var keysPersister: CardKeysPersister

    override fun getTitle(context: Context): String = context.getString(R.string.keys)

    override fun getActionBarOptions(): ActionBarOptions = ActionBarOptions(
            backgroundColorRes = R.color.accent,
            textColorRes = R.color.white
    )

    override fun onCreateView(context: Context): KeysScreenView = KeysScreenView(context, activityOperations, this)

    override fun onShow(context: Context) {
        super.onShow(context)

        activityOperations.menuItemClick
                .autoDisposable(this)
                .subscribe({ menuItem ->
                    when (menuItem.itemId) {
                        R.id.add -> navigator.goTo(AddKeyScreen())
                    }
                })

        loadKeys()
    }

    override fun onUpdateMenu(menu: Menu?) {
        activity.menuInflater.inflate(R.menu.screen_keys, menu)
    }

    override fun onDeleteSelectedItems(items: List<KeyViewModel>) {
        for ((savedKey) in items) {
            keysPersister.delete(savedKey)
        }
        loadKeys()
    }

    override fun createComponent(parentComponent: MainActivity.MainActivityComponent): KeysScreen.KeysComponent =
            DaggerKeysScreen_KeysComponent.builder()
            .mainActivityComponent(parentComponent)
            .build()

    override fun inject(component: KeysScreen.KeysComponent) {
        component.inject(this)
    }

    private fun loadKeys() {
        observeKeys()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(this)
                .subscribe(
                        { keys -> view.setViewModels(keys) },
                        { e -> ErrorUtils.showErrorToast(activity, e) }
                )
    }

    private fun observeKeys(): Single<List<KeyViewModel>> {
        return Single.create<List<SavedKey>> { e ->
            try {
                e.onSuccess(keysPersister.savedKeys)
            } catch (error: Throwable) {
                e.onError(error)
            }
        }.map { savedKeys ->
            savedKeys.map { savedKey -> KeyViewModel(savedKey) }
        }
    }

    @ScreenScope
    @Component(dependencies = arrayOf(MainActivity.MainActivityComponent::class))
    interface KeysComponent {

        fun inject(screen: KeysScreen)
    }
}
