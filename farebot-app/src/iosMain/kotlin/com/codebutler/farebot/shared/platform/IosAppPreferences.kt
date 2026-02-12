package com.codebutler.farebot.shared.platform

import platform.Foundation.NSUserDefaults

class IosAppPreferences : AppPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getBoolean(key: String, default: Boolean): Boolean {
        // NSUserDefaults returns false for unset keys, so track which keys have been set
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            default
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }
}
