package com.codebutler.farebot.shared.platform

interface AppPreferences {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)

    companion object {
        const val KEY_SHOW_UNSUPPORTED = "explore_show_unsupported"
        const val KEY_SHOW_SERIAL_ONLY = "explore_show_serial_only"
        const val KEY_SHOW_KEYS_REQUIRED = "explore_show_keys_required"
        const val KEY_SHOW_EXPERIMENTAL = "explore_show_experimental"
    }
}

/** In-memory fallback for platforms without persistent storage (e.g. JVM previews). */
class InMemoryAppPreferences : AppPreferences {
    private val map = mutableMapOf<String, Boolean>()

    override fun getBoolean(key: String, default: Boolean): Boolean =
        map[key] ?: default

    override fun putBoolean(key: String, value: Boolean) {
        map[key] = value
    }
}
