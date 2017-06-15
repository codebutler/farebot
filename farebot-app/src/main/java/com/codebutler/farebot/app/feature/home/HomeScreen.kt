/*
 * HomeScreen.kt
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

package com.codebutler.farebot.app.feature.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.activity.ActivityOperations
import com.codebutler.farebot.app.core.inject.ScreenScope
import com.codebutler.farebot.app.core.ui.FareBotScreen
import com.codebutler.farebot.app.core.util.ErrorUtils
import com.codebutler.farebot.app.feature.card.CardScreen
import com.codebutler.farebot.app.feature.help.HelpScreen
import com.codebutler.farebot.app.feature.history.HistoryScreen
import com.codebutler.farebot.app.feature.keys.KeysScreen
import com.codebutler.farebot.app.feature.main.MainActivity.MainActivityComponent
import com.codebutler.farebot.app.feature.prefs.FareBotPreferenceActivity
import com.uber.autodispose.ObservableScoper
import dagger.Component
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class HomeScreen : FareBotScreen<HomeScreen.HomeComponent, HomeScreenView>(),
        HomeScreenView.Listener {

    companion object {
        private val URL_ABOUT = Uri.parse("https://codebutler.github.com/farebot")
    }

    @Inject lateinit var activityOperations: ActivityOperations
    @Inject lateinit var cardStream: CardStream

    override fun onCreateView(context: Context): HomeScreenView = HomeScreenView(context, this)

    override fun getTitle(context: Context): String = context.getString(R.string.app_name)

    override fun onShow(context: Context) {
        super.onShow(context)

        activityOperations.menuItemClick
                .to(ObservableScoper<MenuItem>(this))
                .subscribe({ menuItem ->
                    when (menuItem.itemId) {
                        R.id.history -> navigator.goTo(HistoryScreen())
                        R.id.help -> navigator.goTo(HelpScreen())
                        R.id.prefs -> activity.startActivity(FareBotPreferenceActivity.newIntent(activity))
                        R.id.keys -> navigator.goTo(KeysScreen())
                        R.id.about -> activity.startActivity(Intent(Intent.ACTION_VIEW, URL_ABOUT))
                    }
                })

        val adapter = NfcAdapter.getDefaultAdapter(context)
        if (adapter == null) {
            view.showNfcError(HomeScreenView.NfcError.UNAVAILABLE)
        } else if (!adapter.isEnabled) {
            view.showNfcError(HomeScreenView.NfcError.DISABLED)
        } else {
            view.showNfcError(HomeScreenView.NfcError.NONE)
        }

        cardStream.observeCards()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .to(ObservableScoper(this))
                .subscribe { card -> navigator.goTo(CardScreen(card)) }

        cardStream.observeLoading()
                .observeOn(AndroidSchedulers.mainThread())
                .to(ObservableScoper(this))
                .subscribe { loading -> view.showLoading(loading) }

        cardStream.observeErrors()
                .observeOn(AndroidSchedulers.mainThread())
                .to(ObservableScoper(this))
                .subscribe { ex -> ErrorUtils.showErrorAlert(activity, ex) }
    }

    override fun onUpdateMenu(menu: Menu) {
        activity.menuInflater.inflate(R.menu.screen_main, menu)
    }

    override fun onNfcErrorButtonClicked() {
        activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    override fun onSampleButtonClicked() {
        cardStream.emitSample()
    }

    override fun createComponent(parentComponent: MainActivityComponent): HomeComponent
            = DaggerHomeScreen_HomeComponent.builder()
            .mainActivityComponent(parentComponent)
            .build()

    override fun inject(component: HomeComponent) {
        component.inject(this)
    }

    @ScreenScope
    @Component(dependencies = arrayOf(MainActivityComponent::class))
    interface HomeComponent {
        fun inject(homeScreen: HomeScreen)
    }
}
