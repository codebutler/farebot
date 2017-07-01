/*
 * FareBotApplication.kt
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

package com.codebutler.farebot.app.core.app

import android.app.Application
import android.content.SharedPreferences
import android.os.StrictMode
import com.codebutler.farebot.BuildConfig
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import io.fabric.sdk.android.Fabric
import java.util.Date
import javax.inject.Inject

class FareBotApplication : Application() {

    companion object {
        val PREF_LAST_READ_ID = "last_read_id"
        val PREF_LAST_READ_AT = "last_read_at"
    }

    lateinit var component: FareBotApplicationComponent

    @Inject lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())

        component = DaggerFareBotApplicationComponent.builder()
                .application(this)
                .module(FareBotApplicationModule())
                .build()

        component.inject(this)

        if (!BuildConfig.DEBUG) {
            Fabric.with(this, Answers(), Crashlytics())
        } else {
            Fabric.with(this, Answers())
        }
    }

    fun updateTimestamp(tagIdString: String?) {
        val prefs = sharedPreferences.edit()
        prefs.putString(FareBotApplication.PREF_LAST_READ_ID, tagIdString)
        prefs.putLong(FareBotApplication.PREF_LAST_READ_AT, Date().time)
        prefs.apply()
    }
}
