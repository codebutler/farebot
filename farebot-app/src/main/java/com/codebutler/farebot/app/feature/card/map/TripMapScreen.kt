/*
 * TripMapScreen.kt
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

package com.codebutler.farebot.app.feature.card.map

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.inject.ScreenScope
import com.codebutler.farebot.app.core.kotlin.compact
import com.codebutler.farebot.app.core.ui.ActionBarOptions
import com.codebutler.farebot.app.core.ui.FareBotScreen
import com.codebutler.farebot.app.feature.main.MainActivity
import com.codebutler.farebot.transit.Trip

class TripMapScreen(val trip: Trip) : FareBotScreen<TripMapScreen.Component, TripMapScreenView>() {

    override fun getTitle(context: Context): String = context.getString(R.string.map)

    override fun getActionBarOptions(): ActionBarOptions = ActionBarOptions(
            backgroundColorRes = R.color.accent,
            textColorRes = R.color.white
    )

    override fun onCreateView(context: Context): TripMapScreenView = TripMapScreenView(context, trip)

    override fun onShow(context: Context) {
        super.onShow(context)

        view.post {
            (activity as AppCompatActivity).supportActionBar?.apply {
                val resources = context.resources
                setDisplayHomeAsUpEnabled(true)
                title = arrayOf(trip.startStation?.shortStationName, trip.endStation?.shortStationName)
                        .compact()
                        .joinToString(" → ")
                subtitle = arrayOf(trip.getAgencyName(resources), trip.getRouteName(resources))
                        .compact()
                        .joinToString(" ")
            }
        }

        view.onCreate(Bundle())
    }

    override fun onHide(context: Context) {
        super.onHide(context)
        view.onDestroy()
    }

    override fun onResume(context: Context) {
        super.onResume(context)
        view.onStart()
        view.onResume()
    }

    override fun onPause(context: Context) {
        super.onPause(context)
        view.onPause()
        view.onStop()
    }

    override fun onSave(outState: Bundle?) {
        super.onSave(outState)
    }

    override fun onRestore(savedInstanceState: Bundle?) {
        super.onRestore(savedInstanceState)
    }

    override fun createComponent(parentComponent: MainActivity.MainActivityComponent): Component
            = DaggerTripMapScreen_Component.builder()
            .mainActivityComponent(parentComponent)
            .build()

    override fun inject(component: Component) {
        component.inject(this)
    }

    @ScreenScope
    @dagger.Component(dependencies = arrayOf(MainActivity.MainActivityComponent::class))
    interface Component {
        fun inject(screen: TripMapScreen)
    }
}

