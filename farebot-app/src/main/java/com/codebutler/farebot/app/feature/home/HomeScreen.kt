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
import android.nfc.TagLostException
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import android.view.Menu
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.activity.ActivityOperations
import com.codebutler.farebot.app.core.analytics.AnalyticsEventName
import com.codebutler.farebot.app.core.analytics.logAnalyticsEvent
import com.codebutler.farebot.app.core.inject.ScreenScope
import com.codebutler.farebot.app.core.ui.ActionBarOptions
import com.codebutler.farebot.app.core.ui.FareBotScreen
import com.codebutler.farebot.app.core.util.ErrorUtils
import com.codebutler.farebot.app.feature.card.CardScreen
import com.codebutler.farebot.app.feature.help.HelpScreen
import com.codebutler.farebot.app.feature.history.HistoryScreen
import com.codebutler.farebot.app.feature.keys.KeysScreen
import com.codebutler.farebot.app.feature.main.MainActivity.MainActivityComponent
import com.codebutler.farebot.app.feature.prefs.FareBotPreferenceActivity
import com.crashlytics.android.Crashlytics
import com.uber.autodispose.kotlin.autoDisposable
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

    override fun getActionBarOptions(): ActionBarOptions = ActionBarOptions(shadow = false)

    override fun onShow(context: Context) {
        super.onShow(context)

        activityOperations.menuItemClick
                .autoDisposable(this)
                .subscribe({ menuItem ->
                    when (menuItem.itemId) {
                        R.id.history -> navigator.goTo(HistoryScreen())
                        R.id.help -> navigator.goTo(HelpScreen())
                        R.id.prefs -> activity.startActivity(FareBotPreferenceActivity.newIntent(activity))
                        R.id.keys -> navigator.goTo(KeysScreen())
                        R.id.about -> {
                            activity.startActivity(Intent(Intent.ACTION_VIEW, URL_ABOUT))
                        }
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
                .autoDisposable(this)
                .subscribe { card ->
                    logAnalyticsEvent(AnalyticsEventName.SCAN_CARD, card.cardType().toString())
                    navigator.goTo(CardScreen(card))
                }

        cardStream.observeLoading()
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(this)
                .subscribe { loading -> view.showLoading(loading) }

        cardStream.observeErrors()
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(this)
                .subscribe { ex ->
                    logAnalyticsEvent(AnalyticsEventName.SCAN_CARD_ERROR, ErrorUtils.getErrorMessage(ex))
                    when (ex) {
                        is CardStream.CardUnauthorizedException -> AlertDialog.Builder(activity)
                                .setTitle(R.string.locked_card)
                                .setMessage(R.string.keys_required)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        is TagLostException -> AlertDialog.Builder(activity)
                                .setTitle(R.string.tag_lost)
                                .setMessage(R.string.tag_lost_message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        else -> {
                            Crashlytics.logException(ex)
                            ErrorUtils.showErrorAlert(activity, ex)
                        }
                    }
                }
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

    override fun createComponent(parentComponent: MainActivityComponent): HomeComponent =
            DaggerHomeScreen_HomeComponent.builder()
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
