/*
 * MainActivity.kt
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

package com.codebutler.farebot.app.feature.main

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.activity.ActivityOperations
import com.codebutler.farebot.app.core.activity.ActivityResult
import com.codebutler.farebot.app.core.activity.RequestPermissionsResult
import com.codebutler.farebot.app.core.app.FareBotApplication
import com.codebutler.farebot.app.core.app.FareBotApplicationComponent
import com.codebutler.farebot.app.core.inject.ActivityScope
import com.codebutler.farebot.app.core.kotlin.bindView
import com.codebutler.farebot.app.core.nfc.NfcStream
import com.codebutler.farebot.app.core.nfc.TagReaderFactory
import com.codebutler.farebot.app.core.serialize.CardKeysSerializer
import com.codebutler.farebot.app.core.transit.TransitFactoryRegistry
import com.codebutler.farebot.app.core.ui.FareBotCrossfadeTransition
import com.codebutler.farebot.app.core.ui.FareBotScreen
import com.codebutler.farebot.app.core.util.ExportHelper
import com.codebutler.farebot.app.feature.home.CardStream
import com.codebutler.farebot.app.feature.home.HomeScreen
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.CardPersister
import com.jakewharton.rxrelay2.PublishRelay
import com.wealthfront.magellan.ActionBarConfig
import com.wealthfront.magellan.NavigationListener
import com.wealthfront.magellan.Navigator
import com.wealthfront.magellan.Screen
import com.wealthfront.magellan.ScreenLifecycleListener
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Inject

class MainActivity : AppCompatActivity(),
        ScreenLifecycleListener,
        NavigationListener {

    @Inject internal lateinit var navigator: Navigator
    @Inject internal lateinit var nfcStream: NfcStream

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    private val activityResultRelay = PublishRelay.create<ActivityResult>()
    private val handler = Handler()
    private val menuItemClickRelay = PublishRelay.create<MenuItem>()
    private val permissionsResultRelay = PublishRelay.create<RequestPermissionsResult>()

    private val toolbarElevation: Float by lazy { resources.getDimensionPixelSize(R.dimen.toolbar_elevation).toFloat() }

    val component: MainActivityComponent by lazy {
        DaggerMainActivity_MainActivityComponent.builder()
                .applicationComponent((application as FareBotApplication).component)
                .activity(this)
                .mainActivityModule(MainActivityModule())
                .activityOperations(ActivityOperations(
                        this,
                        activityResultRelay.hide(),
                        menuItemClickRelay.hide(),
                        permissionsResultRelay.hide()))
                .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        component.inject(this)
        navigator.addLifecycleListener(this)
        nfcStream.onCreate(this, savedInstanceState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        navigator.onCreate(this, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        navigator.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        navigator.onResume(this)
        nfcStream.onResume()
    }

    override fun onPause() {
        super.onPause()
        navigator.onPause(this)
        nfcStream.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        navigator.removeLifecycleListener(this)
        navigator.onDestroy(this)
    }

    override fun onBackPressed() {
        if (!navigator.handleBack()) {
            super.onBackPressed()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        navigator.onCreateOptionsMenu(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        navigator.onPrepareOptionsMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        menuItemClickRelay.accept(item)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handler.post {
            activityResultRelay.accept(ActivityResult(requestCode, resultCode, data))
        }
    }

    override fun onNavigate(actionBarConfig: ActionBarConfig) {
        toolbar.visibility = if (actionBarConfig.visible()) View.VISIBLE else View.GONE
    }

    override fun onShow(screen: Screen<*>) {
        @ColorInt
        fun getColor(@ColorRes colorRes: Int?, @ColorInt defaultColor: Int)
                = if (colorRes == null) defaultColor else ResourcesCompat.getColor(resources, colorRes, theme)

        supportActionBar?.setDisplayHomeAsUpEnabled(!navigator.atRoot())

        val options = (screen as FareBotScreen<*, *>).getActionBarOptions()
        toolbar.setTitleTextColor(getColor(options.textColorRes, Color.BLACK))
        toolbar.setBackgroundColor(getColor(options.backgroundColorRes, Color.TRANSPARENT))
        toolbar.title = screen.getTitle(this)
        toolbar.subtitle = null
        ViewCompat.setElevation(toolbar, if (options.shadow) toolbarElevation else 0f)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsResultRelay.accept(RequestPermissionsResult(requestCode, permissions, grantResults))
    }

    override fun onHide(screen: Screen<*>?) { }

    @Module
    class MainActivityModule {

        @Provides
        @ActivityScope
        fun provideNfcTagStream(activity: MainActivity): NfcStream = NfcStream(activity)

        @Provides
        @ActivityScope
        fun provideCardStream(
                application: FareBotApplication,
                cardPersister: CardPersister,
                cardSerializer: CardSerializer,
                cardKeysPersister: CardKeysPersister,
                cardKeysSerializer: CardKeysSerializer,
                nfcStream: NfcStream,
                tagReaderFactory: TagReaderFactory): CardStream {
            return CardStream(
                    application,
                    cardPersister,
                    cardSerializer,
                    cardKeysPersister,
                    cardKeysSerializer,
                    nfcStream,
                    tagReaderFactory)
        }

        @Provides
        @ActivityScope
        fun provideNavigator(activity: MainActivity): Navigator = Navigator.withRoot(HomeScreen())
                .transition(FareBotCrossfadeTransition(activity))
                .build()
    }

    @ActivityScope
    @Component(dependencies = arrayOf(FareBotApplicationComponent::class), modules = arrayOf(MainActivityModule::class))
    interface MainActivityComponent {

        fun activityOperations() : ActivityOperations

        fun application() : FareBotApplication

        fun cardPersister() : CardPersister

        fun cardSerializer() : CardSerializer

        fun cardStream() : CardStream

        fun exportHelper() : ExportHelper

        fun sharedPreferences(): SharedPreferences

        fun tagReaderFactory(): TagReaderFactory

        fun transitFactoryRegistry() : TransitFactoryRegistry

        fun inject(mainActivity : MainActivity)

        @Component.Builder
        interface Builder {

            fun applicationComponent(applicationComponent: FareBotApplicationComponent): Builder

            fun mainActivityModule(mainActivityModule: MainActivityModule): Builder

            @BindsInstance
            fun activity(activity: MainActivity): Builder

            @BindsInstance
            fun activityOperations(activityOperations: ActivityOperations): Builder

            fun build(): MainActivityComponent
        }
    }
}
