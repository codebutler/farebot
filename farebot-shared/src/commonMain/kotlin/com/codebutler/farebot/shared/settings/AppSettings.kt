/*
 * AppSettings.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2019 Google
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.shared.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * Multiplatform settings interface for FareBot.
 * Platform-specific implementations use SharedPreferences (Android) or NSUserDefaults (iOS).
 */
expect class AppSettings {

    // ========== Raw Data Display ==========

    /**
     * Level of raw/debug information to display.
     * NONE = normal user view, UNKNOWN_ONLY = show unknown fields, ALL = show everything
     */
    val rawLevel: StateFlow<RawLevel>
    fun setRawLevel(level: RawLevel)

    /**
     * Show raw station IDs alongside station names.
     * Useful for debugging station databases.
     */
    val showRawStationIds: StateFlow<Boolean>
    fun setShowRawStationIds(enabled: Boolean)

    // ========== Obfuscation Settings (for exports/screenshots) ==========

    /**
     * Hide card serial numbers in the UI.
     */
    val hideCardNumbers: StateFlow<Boolean>
    fun setHideCardNumbers(enabled: Boolean)

    /**
     * Obfuscate balance values in the UI.
     */
    val obfuscateBalance: StateFlow<Boolean>
    fun setObfuscateBalance(enabled: Boolean)

    /**
     * Obfuscate trip fare amounts in the UI.
     */
    val obfuscateTripFares: StateFlow<Boolean>
    fun setObfuscateTripFares(enabled: Boolean)

    /**
     * Obfuscate trip dates in the UI.
     */
    val obfuscateTripDates: StateFlow<Boolean>
    fun setObfuscateTripDates(enabled: Boolean)

    /**
     * Obfuscate trip times in the UI.
     */
    val obfuscateTripTimes: StateFlow<Boolean>
    fun setObfuscateTripTimes(enabled: Boolean)

    // ========== Language & Localization ==========

    /**
     * Show both local language and English names for stations/places.
     */
    val showBothLocalAndEnglish: StateFlow<Boolean>
    fun setShowBothLocalAndEnglish(enabled: Boolean)

    /**
     * Convert timestamps to local timezone instead of card's timezone.
     */
    val convertTimezone: StateFlow<Boolean>
    fun setConvertTimezone(enabled: Boolean)

    /**
     * Use local place names (transliterated) instead of original script.
     */
    val localisePlaces: StateFlow<Boolean>
    fun setLocalisePlaces(enabled: Boolean)

    /**
     * Language code override (empty = system default).
     */
    val languageOverride: StateFlow<String>
    fun setLanguageOverride(languageCode: String)

    // ========== Debug Options ==========

    /**
     * Enable extra debug logging.
     */
    val debugLogging: StateFlow<Boolean>
    fun setDebugLogging(enabled: Boolean)

    /**
     * Show debug spans in text (developer feature).
     */
    val debugSpans: StateFlow<Boolean>
    fun setDebugSpans(enabled: Boolean)

    /**
     * Use ISO 8601 format for timestamps instead of localized format.
     */
    val useIsoDateTimeStamps: StateFlow<Boolean>
    fun setUseIsoDateTimeStamps(enabled: Boolean)

    // ========== Card Reading Options ==========

    /**
     * Number of authentication retries for MIFARE Classic cards.
     */
    val mfcAuthRetry: StateFlow<Int>
    fun setMfcAuthRetry(count: Int)

    /**
     * Fallback reader type for MIFARE Classic cards.
     */
    val mfcFallbackReader: StateFlow<String>
    fun setMfcFallbackReader(reader: String)

    /**
     * Attempt to retrieve LEAP card keys from the network.
     */
    val retrieveLeapKeys: StateFlow<Boolean>
    fun setRetrieveLeapKeys(enabled: Boolean)

    // ========== Accessibility ==========

    /**
     * Speak balance aloud when a card is scanned (TTS).
     */
    val speakBalance: StateFlow<Boolean>
    fun setSpeakBalance(enabled: Boolean)

    // ========== Convenience Accessors ==========

    /**
     * Current language code (system default or override).
     */
    val language: String

    /**
     * Current region/country code from device settings.
     */
    val region: String?

    /**
     * App version string.
     */
    val appVersion: String

}

/**
 * Constants for AppSettings preference keys and default values.
 * Separated from expect class because expect companion objects cannot have initializers.
 */
object AppSettingsKeys {
    // Preference keys
    const val PREF_RAW_LEVEL = "pref_raw_level"
    const val PREF_SHOW_RAW_IDS = "pref_show_raw_ids"
    const val PREF_HIDE_CARD_NUMBERS = "pref_hide_card_numbers"
    const val PREF_OBFUSCATE_BALANCE = "pref_obfuscate_balance"
    const val PREF_OBFUSCATE_TRIP_FARES = "pref_obfuscate_trip_fares"
    const val PREF_OBFUSCATE_TRIP_DATES = "pref_obfuscate_trip_dates"
    const val PREF_OBFUSCATE_TRIP_TIMES = "pref_obfuscate_trip_times"
    const val PREF_SHOW_LOCAL_AND_ENGLISH = "pref_show_local_and_english"
    const val PREF_CONVERT_TIMEZONES = "pref_convert_timezones"
    const val PREF_LOCALISE_PLACES = "pref_localise_places"
    const val PREF_LANG_OVERRIDE = "pref_lang_override"
    const val PREF_DEBUG_LOGGING = "pref_debug_logging"
    const val PREF_DEBUG_SPANS = "pref_debug_spans"
    const val PREF_USE_ISO_DATETIME = "pref_use_iso_datetime"
    const val PREF_MFC_AUTHRETRY = "pref_mfc_authretry"
    const val PREF_MFC_FALLBACK = "pref_mfc_fallback"
    const val PREF_RETRIEVE_LEAP_KEYS = "pref_retrieve_leap_keys"
    const val PREF_SPEAK_BALANCE = "pref_key_speak_balance"

    // Default values
    const val DEFAULT_MFC_AUTH_RETRY = 5
    const val DEFAULT_MFC_FALLBACK = "null"
}
