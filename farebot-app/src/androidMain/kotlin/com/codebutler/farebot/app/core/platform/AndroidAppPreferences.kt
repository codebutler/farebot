package com.codebutler.farebot.app.core.platform

import android.content.Context
import com.codebutler.farebot.shared.platform.AppPreferences

class AndroidAppPreferences(context: Context) : AppPreferences {
    private val prefs = context.getSharedPreferences("farebot_prefs", Context.MODE_PRIVATE)

    override fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}
