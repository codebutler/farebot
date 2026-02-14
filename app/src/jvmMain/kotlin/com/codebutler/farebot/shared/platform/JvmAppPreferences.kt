package com.codebutler.farebot.shared.platform

import java.util.prefs.Preferences

class JvmAppPreferences : AppPreferences {
    private val prefs = Preferences.userNodeForPackage(AppPreferences::class.java)

    override fun getBoolean(
        key: String,
        default: Boolean,
    ): Boolean = prefs.getBoolean(key, default)

    override fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        prefs.putBoolean(key, value)
    }
}
