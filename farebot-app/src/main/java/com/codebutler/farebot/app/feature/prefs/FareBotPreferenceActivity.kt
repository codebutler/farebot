/*
 * FareBotPreferenceActivity.kt
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

package com.codebutler.farebot.app.feature.prefs

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.view.MenuItem
import com.codebutler.farebot.R
import com.codebutler.farebot.app.feature.bg.BackgroundTagActivity

@Suppress("DEPRECATION")
class FareBotPreferenceActivity : PreferenceActivity(), Preference.OnPreferenceChangeListener {

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, FareBotPreferenceActivity::class.java)
    }

    private lateinit var preferenceLaunchFromBackground: CheckBoxPreference

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.prefs)

        actionBar?.setDisplayHomeAsUpEnabled(true)

        preferenceLaunchFromBackground = findPreference("pref_launch_from_background") as CheckBoxPreference
        preferenceLaunchFromBackground.isChecked = launchFromBgEnabled
        preferenceLaunchFromBackground.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference === preferenceLaunchFromBackground) {
            launchFromBgEnabled = newValue as Boolean
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private var launchFromBgEnabled: Boolean
        get() {
            val componentName = ComponentName(this, BackgroundTagActivity::class.java)
            val componentEnabledSetting = packageManager.getComponentEnabledSetting(componentName)
            return componentEnabledSetting == COMPONENT_ENABLED_STATE_ENABLED
        }
        set(enabled) {
            val componentName = ComponentName(this, BackgroundTagActivity::class.java)
            val newState = if (enabled) COMPONENT_ENABLED_STATE_ENABLED else COMPONENT_ENABLED_STATE_DISABLED
            packageManager.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
        }
}
