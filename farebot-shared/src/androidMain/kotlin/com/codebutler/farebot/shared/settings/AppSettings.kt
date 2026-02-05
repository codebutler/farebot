/*
 * AppSettings.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

import android.content.Context
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
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

actual class AppSettings(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "${context.packageName}_preferences",
        Context.MODE_PRIVATE
    )

    // ========== Raw Data Display ==========

    private val _rawLevel = MutableStateFlow(loadRawLevel())
    actual val rawLevel: StateFlow<RawLevel> = _rawLevel.asStateFlow()

    actual fun setRawLevel(level: RawLevel) {
        prefs.edit().putString(PREF_RAW_LEVEL, level.toString()).apply()
        _rawLevel.value = level
    }

    private fun loadRawLevel(): RawLevel {
        val value = prefs.getString(PREF_RAW_LEVEL, RawLevel.NONE.toString()) ?: RawLevel.NONE.toString()
        return RawLevel.fromString(value) ?: RawLevel.NONE
    }

    private val _showRawStationIds = MutableStateFlow(prefs.getBoolean(PREF_SHOW_RAW_IDS, false))
    actual val showRawStationIds: StateFlow<Boolean> = _showRawStationIds.asStateFlow()

    actual fun setShowRawStationIds(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_SHOW_RAW_IDS, enabled).apply()
        _showRawStationIds.value = enabled
    }

    // ========== Obfuscation Settings ==========

    private val _hideCardNumbers = MutableStateFlow(prefs.getBoolean(PREF_HIDE_CARD_NUMBERS, false))
    actual val hideCardNumbers: StateFlow<Boolean> = _hideCardNumbers.asStateFlow()

    actual fun setHideCardNumbers(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_HIDE_CARD_NUMBERS, enabled).apply()
        _hideCardNumbers.value = enabled
    }

    private val _obfuscateBalance = MutableStateFlow(prefs.getBoolean(PREF_OBFUSCATE_BALANCE, false))
    actual val obfuscateBalance: StateFlow<Boolean> = _obfuscateBalance.asStateFlow()

    actual fun setObfuscateBalance(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_OBFUSCATE_BALANCE, enabled).apply()
        _obfuscateBalance.value = enabled
    }

    private val _obfuscateTripFares = MutableStateFlow(prefs.getBoolean(PREF_OBFUSCATE_TRIP_FARES, false))
    actual val obfuscateTripFares: StateFlow<Boolean> = _obfuscateTripFares.asStateFlow()

    actual fun setObfuscateTripFares(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_OBFUSCATE_TRIP_FARES, enabled).apply()
        _obfuscateTripFares.value = enabled
    }

    private val _obfuscateTripDates = MutableStateFlow(prefs.getBoolean(PREF_OBFUSCATE_TRIP_DATES, false))
    actual val obfuscateTripDates: StateFlow<Boolean> = _obfuscateTripDates.asStateFlow()

    actual fun setObfuscateTripDates(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_OBFUSCATE_TRIP_DATES, enabled).apply()
        _obfuscateTripDates.value = enabled
    }

    private val _obfuscateTripTimes = MutableStateFlow(prefs.getBoolean(PREF_OBFUSCATE_TRIP_TIMES, false))
    actual val obfuscateTripTimes: StateFlow<Boolean> = _obfuscateTripTimes.asStateFlow()

    actual fun setObfuscateTripTimes(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_OBFUSCATE_TRIP_TIMES, enabled).apply()
        _obfuscateTripTimes.value = enabled
    }

    // ========== Language & Localization ==========

    private val _showBothLocalAndEnglish = MutableStateFlow(prefs.getBoolean(PREF_SHOW_LOCAL_AND_ENGLISH, false))
    actual val showBothLocalAndEnglish: StateFlow<Boolean> = _showBothLocalAndEnglish.asStateFlow()

    actual fun setShowBothLocalAndEnglish(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_SHOW_LOCAL_AND_ENGLISH, enabled).apply()
        _showBothLocalAndEnglish.value = enabled
    }

    private val _convertTimezone = MutableStateFlow(prefs.getBoolean(PREF_CONVERT_TIMEZONES, false))
    actual val convertTimezone: StateFlow<Boolean> = _convertTimezone.asStateFlow()

    actual fun setConvertTimezone(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_CONVERT_TIMEZONES, enabled).apply()
        _convertTimezone.value = enabled
    }

    private val _localisePlaces = MutableStateFlow(prefs.getBoolean(PREF_LOCALISE_PLACES, false))
    actual val localisePlaces: StateFlow<Boolean> = _localisePlaces.asStateFlow()

    actual fun setLocalisePlaces(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_LOCALISE_PLACES, enabled).apply()
        _localisePlaces.value = enabled
    }

    private val _languageOverride = MutableStateFlow(prefs.getString(PREF_LANG_OVERRIDE, "") ?: "")
    actual val languageOverride: StateFlow<String> = _languageOverride.asStateFlow()

    actual fun setLanguageOverride(languageCode: String) {
        prefs.edit().putString(PREF_LANG_OVERRIDE, languageCode).apply()
        _languageOverride.value = languageCode
    }

    // ========== Debug Options ==========

    private val _debugLogging = MutableStateFlow(prefs.getBoolean(PREF_DEBUG_LOGGING, false))
    actual val debugLogging: StateFlow<Boolean> = _debugLogging.asStateFlow()

    actual fun setDebugLogging(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_DEBUG_LOGGING, enabled).apply()
        _debugLogging.value = enabled
    }

    private val _debugSpans = MutableStateFlow(prefs.getBoolean(PREF_DEBUG_SPANS, false))
    actual val debugSpans: StateFlow<Boolean> = _debugSpans.asStateFlow()

    actual fun setDebugSpans(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_DEBUG_SPANS, enabled).apply()
        _debugSpans.value = enabled
    }

    private val _useIsoDateTimeStamps = MutableStateFlow(prefs.getBoolean(PREF_USE_ISO_DATETIME, false))
    actual val useIsoDateTimeStamps: StateFlow<Boolean> = _useIsoDateTimeStamps.asStateFlow()

    actual fun setUseIsoDateTimeStamps(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_USE_ISO_DATETIME, enabled).apply()
        _useIsoDateTimeStamps.value = enabled
    }

    // ========== Card Reading Options ==========

    private val _mfcAuthRetry = MutableStateFlow(prefs.getInt(PREF_MFC_AUTHRETRY, DEFAULT_MFC_AUTH_RETRY))
    actual val mfcAuthRetry: StateFlow<Int> = _mfcAuthRetry.asStateFlow()

    actual fun setMfcAuthRetry(count: Int) {
        prefs.edit().putInt(PREF_MFC_AUTHRETRY, count).apply()
        _mfcAuthRetry.value = count
    }

    private val _mfcFallbackReader = MutableStateFlow(
        prefs.getString(PREF_MFC_FALLBACK, DEFAULT_MFC_FALLBACK)?.lowercase(Locale.US) ?: DEFAULT_MFC_FALLBACK
    )
    actual val mfcFallbackReader: StateFlow<String> = _mfcFallbackReader.asStateFlow()

    actual fun setMfcFallbackReader(reader: String) {
        prefs.edit().putString(PREF_MFC_FALLBACK, reader).apply()
        _mfcFallbackReader.value = reader.lowercase(Locale.US)
    }

    private val _retrieveLeapKeys = MutableStateFlow(prefs.getBoolean(PREF_RETRIEVE_LEAP_KEYS, false))
    actual val retrieveLeapKeys: StateFlow<Boolean> = _retrieveLeapKeys.asStateFlow()

    actual fun setRetrieveLeapKeys(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_RETRIEVE_LEAP_KEYS, enabled).apply()
        _retrieveLeapKeys.value = enabled
    }

    // ========== Accessibility ==========

    private val _speakBalance = MutableStateFlow(prefs.getBoolean(PREF_SPEAK_BALANCE, false))
    actual val speakBalance: StateFlow<Boolean> = _speakBalance.asStateFlow()

    actual fun setSpeakBalance(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_SPEAK_BALANCE, enabled).apply()
        _speakBalance.value = enabled
    }

    // ========== Convenience Accessors ==========

    actual val language: String
        get() = Locale.getDefault().language

    actual val region: String?
        get() {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE)
            if (tm is TelephonyManager && (
                tm.phoneType == TelephonyManager.PHONE_TYPE_GSM ||
                tm.phoneType == TelephonyManager.PHONE_TYPE_CDMA
            )) {
                val netCountry = tm.networkCountryIso
                if (netCountry != null && netCountry.length == 2) {
                    return netCountry.uppercase(Locale.US)
                }

                val simCountry = tm.simCountryIso
                if (simCountry != null && simCountry.length == 2) {
                    return simCountry.uppercase(Locale.US)
                }
            }

            // Fall back to using the Locale settings
            return Locale.getDefault().country.uppercase(Locale.US)
        }

    actual val appVersion: String
        get() = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }

}
