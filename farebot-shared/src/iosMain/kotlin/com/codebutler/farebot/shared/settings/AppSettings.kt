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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.preferredLanguages
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_RAW_LEVEL
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_SHOW_RAW_IDS
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_HIDE_CARD_NUMBERS
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_OBFUSCATE_BALANCE
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_OBFUSCATE_TRIP_FARES
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_OBFUSCATE_TRIP_DATES
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_OBFUSCATE_TRIP_TIMES
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_SHOW_LOCAL_AND_ENGLISH
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_CONVERT_TIMEZONES
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_LOCALISE_PLACES
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_LANG_OVERRIDE
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_DEBUG_LOGGING
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_DEBUG_SPANS
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_USE_ISO_DATETIME
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_MFC_AUTHRETRY
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_MFC_FALLBACK
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_RETRIEVE_LEAP_KEYS
import com.codebutler.farebot.shared.settings.AppSettingsKeys.PREF_SPEAK_BALANCE
import com.codebutler.farebot.shared.settings.AppSettingsKeys.DEFAULT_MFC_AUTH_RETRY
import com.codebutler.farebot.shared.settings.AppSettingsKeys.DEFAULT_MFC_FALLBACK

actual class AppSettings {

    private val defaults = NSUserDefaults.standardUserDefaults

    // ========== Raw Data Display ==========

    private val _rawLevel = MutableStateFlow(loadRawLevel())
    actual val rawLevel: StateFlow<RawLevel> = _rawLevel.asStateFlow()

    actual fun setRawLevel(level: RawLevel) {
        defaults.setObject(level.toString(), forKey = PREF_RAW_LEVEL)
        _rawLevel.value = level
    }

    private fun loadRawLevel(): RawLevel {
        val value = defaults.stringForKey(PREF_RAW_LEVEL) ?: RawLevel.NONE.toString()
        return RawLevel.fromString(value) ?: RawLevel.NONE
    }

    private val _showRawStationIds = MutableStateFlow(defaults.boolForKey(PREF_SHOW_RAW_IDS))
    actual val showRawStationIds: StateFlow<Boolean> = _showRawStationIds.asStateFlow()

    actual fun setShowRawStationIds(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_SHOW_RAW_IDS)
        _showRawStationIds.value = enabled
    }

    // ========== Obfuscation Settings ==========

    private val _hideCardNumbers = MutableStateFlow(defaults.boolForKey(PREF_HIDE_CARD_NUMBERS))
    actual val hideCardNumbers: StateFlow<Boolean> = _hideCardNumbers.asStateFlow()

    actual fun setHideCardNumbers(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_HIDE_CARD_NUMBERS)
        _hideCardNumbers.value = enabled
    }

    private val _obfuscateBalance = MutableStateFlow(defaults.boolForKey(PREF_OBFUSCATE_BALANCE))
    actual val obfuscateBalance: StateFlow<Boolean> = _obfuscateBalance.asStateFlow()

    actual fun setObfuscateBalance(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_OBFUSCATE_BALANCE)
        _obfuscateBalance.value = enabled
    }

    private val _obfuscateTripFares = MutableStateFlow(defaults.boolForKey(PREF_OBFUSCATE_TRIP_FARES))
    actual val obfuscateTripFares: StateFlow<Boolean> = _obfuscateTripFares.asStateFlow()

    actual fun setObfuscateTripFares(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_OBFUSCATE_TRIP_FARES)
        _obfuscateTripFares.value = enabled
    }

    private val _obfuscateTripDates = MutableStateFlow(defaults.boolForKey(PREF_OBFUSCATE_TRIP_DATES))
    actual val obfuscateTripDates: StateFlow<Boolean> = _obfuscateTripDates.asStateFlow()

    actual fun setObfuscateTripDates(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_OBFUSCATE_TRIP_DATES)
        _obfuscateTripDates.value = enabled
    }

    private val _obfuscateTripTimes = MutableStateFlow(defaults.boolForKey(PREF_OBFUSCATE_TRIP_TIMES))
    actual val obfuscateTripTimes: StateFlow<Boolean> = _obfuscateTripTimes.asStateFlow()

    actual fun setObfuscateTripTimes(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_OBFUSCATE_TRIP_TIMES)
        _obfuscateTripTimes.value = enabled
    }

    // ========== Language & Localization ==========

    private val _showBothLocalAndEnglish = MutableStateFlow(defaults.boolForKey(PREF_SHOW_LOCAL_AND_ENGLISH))
    actual val showBothLocalAndEnglish: StateFlow<Boolean> = _showBothLocalAndEnglish.asStateFlow()

    actual fun setShowBothLocalAndEnglish(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_SHOW_LOCAL_AND_ENGLISH)
        _showBothLocalAndEnglish.value = enabled
    }

    private val _convertTimezone = MutableStateFlow(defaults.boolForKey(PREF_CONVERT_TIMEZONES))
    actual val convertTimezone: StateFlow<Boolean> = _convertTimezone.asStateFlow()

    actual fun setConvertTimezone(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_CONVERT_TIMEZONES)
        _convertTimezone.value = enabled
    }

    private val _localisePlaces = MutableStateFlow(defaults.boolForKey(PREF_LOCALISE_PLACES))
    actual val localisePlaces: StateFlow<Boolean> = _localisePlaces.asStateFlow()

    actual fun setLocalisePlaces(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_LOCALISE_PLACES)
        _localisePlaces.value = enabled
    }

    private val _languageOverride = MutableStateFlow(defaults.stringForKey(PREF_LANG_OVERRIDE) ?: "")
    actual val languageOverride: StateFlow<String> = _languageOverride.asStateFlow()

    actual fun setLanguageOverride(languageCode: String) {
        defaults.setObject(languageCode, forKey = PREF_LANG_OVERRIDE)
        _languageOverride.value = languageCode
    }

    // ========== Debug Options ==========

    private val _debugLogging = MutableStateFlow(defaults.boolForKey(PREF_DEBUG_LOGGING))
    actual val debugLogging: StateFlow<Boolean> = _debugLogging.asStateFlow()

    actual fun setDebugLogging(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_DEBUG_LOGGING)
        _debugLogging.value = enabled
    }

    private val _debugSpans = MutableStateFlow(defaults.boolForKey(PREF_DEBUG_SPANS))
    actual val debugSpans: StateFlow<Boolean> = _debugSpans.asStateFlow()

    actual fun setDebugSpans(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_DEBUG_SPANS)
        _debugSpans.value = enabled
    }

    private val _useIsoDateTimeStamps = MutableStateFlow(defaults.boolForKey(PREF_USE_ISO_DATETIME))
    actual val useIsoDateTimeStamps: StateFlow<Boolean> = _useIsoDateTimeStamps.asStateFlow()

    actual fun setUseIsoDateTimeStamps(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_USE_ISO_DATETIME)
        _useIsoDateTimeStamps.value = enabled
    }

    // ========== Card Reading Options ==========

    private val _mfcAuthRetry = MutableStateFlow(
        defaults.integerForKey(PREF_MFC_AUTHRETRY).takeIf { it > 0 }?.toInt() ?: DEFAULT_MFC_AUTH_RETRY
    )
    actual val mfcAuthRetry: StateFlow<Int> = _mfcAuthRetry.asStateFlow()

    actual fun setMfcAuthRetry(count: Int) {
        defaults.setInteger(count.toLong(), forKey = PREF_MFC_AUTHRETRY)
        _mfcAuthRetry.value = count
    }

    private val _mfcFallbackReader = MutableStateFlow(
        defaults.stringForKey(PREF_MFC_FALLBACK)?.lowercase() ?: DEFAULT_MFC_FALLBACK
    )
    actual val mfcFallbackReader: StateFlow<String> = _mfcFallbackReader.asStateFlow()

    actual fun setMfcFallbackReader(reader: String) {
        defaults.setObject(reader, forKey = PREF_MFC_FALLBACK)
        _mfcFallbackReader.value = reader.lowercase()
    }

    private val _retrieveLeapKeys = MutableStateFlow(defaults.boolForKey(PREF_RETRIEVE_LEAP_KEYS))
    actual val retrieveLeapKeys: StateFlow<Boolean> = _retrieveLeapKeys.asStateFlow()

    actual fun setRetrieveLeapKeys(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_RETRIEVE_LEAP_KEYS)
        _retrieveLeapKeys.value = enabled
    }

    // ========== Accessibility ==========

    private val _speakBalance = MutableStateFlow(defaults.boolForKey(PREF_SPEAK_BALANCE))
    actual val speakBalance: StateFlow<Boolean> = _speakBalance.asStateFlow()

    actual fun setSpeakBalance(enabled: Boolean) {
        defaults.setBool(enabled, forKey = PREF_SPEAK_BALANCE)
        _speakBalance.value = enabled
    }

    // ========== Convenience Accessors ==========

    actual val language: String
        get() {
            val override = _languageOverride.value
            if (override.isNotEmpty()) return override
            val preferred = NSLocale.preferredLanguages.firstOrNull() as? String
            return preferred ?: "en"
        }

    actual val region: String?
        get() = NSLocale.currentLocale.countryCode

    actual val appVersion: String
        get() = (NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String) ?: "unknown"

}
